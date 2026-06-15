package com.example.ticketing.queue.service;

import com.example.ticketing.global.exception.BusinessException;
import com.example.ticketing.global.exception.NotFoundException;
import com.example.ticketing.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueService {
    private final StringRedisTemplate redisTemplate;
    private static final String WAITING_QUEUE_KEY_PREFIX = "queue:waiting:";
    private static final String ACTIVE_QUEUE_KEY_PREFIX = "queue:active:";
    private static final String USER_TOKEN_KEY_PREFIX = "queue:user-token:";
    private static final String TOKEN_KEY_PREFIX = "queue:token:";
    private static final Duration QUEUE_TOKEN_TTL = Duration.ofMinutes(10);

    @Value("${queue.max-active-users:${queue.active-entry-limit:5000}}")
    private int maxActiveUsers;

    @Value("${queue.promotion-batch-size:100}")
    private int promotionBatchSize;

    // ⚠️ 부하테스트(A 방식) 전용: 큐토큰 검증 우회 스위치.
    //    기본 false. dev/loadtest 프로파일에서만 QUEUE_TOKEN_BYPASS=true 로 켤 것.
    //    운영 프로파일에선 절대 true 금지 (대기열 자체가 무력화됨).
    @Value("${queue.token.bypass:false}")
    private boolean queueTokenBypass;

    @PostConstruct
    void warnIfBypassEnabled() {
        if (queueTokenBypass) {
            log.warn("==== QUEUE TOKEN BYPASS 활성화됨 (부하테스트 모드). 운영 환경이면 즉시 끌 것! ====");
        }
    }

    public String joinQueue(Long showId, String userId) {
        double score = Instant.now().toEpochMilli();

        // 같은 사용자가 다시 진입하면 score를 갱신해 대기열 뒤로 보냄
        redisTemplate.opsForZSet().add(waitingQueueKey(showId), userId, score);
        return "JOINED";
    }

    public synchronized QueueStatusResponse enterQueue(Long showId, String userId) {
        cleanupExpiredActiveUsers(showId);

        if (isActiveUser(showId, userId)) {
            return new QueueStatusResponse("ACTIVE", null, true, issueQueueToken(showId, userId));
        }

        Long rank = redisTemplate.opsForZSet().rank(waitingQueueKey(showId), userId);
        if (rank != null) {
            return new QueueStatusResponse("WAITING", rank + 1, false, null);
        }

        if (canEnterImmediately(showId)) {
            // 대기자가 없고 ACTIVE 여유가 있으면 대기열 화면 없이 바로 입장 토큰을 발급함
            redisTemplate.opsForZSet().add(activeQueueKey(showId), userId, activeExpiresAt());
            return new QueueStatusResponse("ACTIVE", null, true, issueQueueToken(showId, userId));
        }

        redisTemplate.opsForZSet().add(waitingQueueKey(showId), userId, Instant.now().toEpochMilli());
        Long newRank = redisTemplate.opsForZSet().rank(waitingQueueKey(showId), userId);
        return new QueueStatusResponse("WAITING", newRank == null ? null : newRank + 1, false, null);
    }

    public synchronized QueueStatusResponse getStatus(Long showId, String userId) {
        cleanupExpiredActiveUsers(showId);

        // Redis ZSET rank는 0부터 시작하므로 응답에서는 1을 더함
        Long rank = redisTemplate.opsForZSet().rank(waitingQueueKey(showId), userId);
        if (rank == null && isActiveUser(showId, userId)) {
            return new QueueStatusResponse("ACTIVE", null, true, issueQueueToken(showId, userId));
        }

        if (rank == null) {
            throw new NotFoundException(ErrorCode.QUEUE_NOT_FOUND);
        }

        return new QueueStatusResponse("WAITING", rank + 1, false, null);
    }

    public void leaveQueue(Long showId, String userId) {
        // 사용자를 해당 공연의 대기열 ZSET에서 제거
        redisTemplate.opsForZSet().remove(waitingQueueKey(showId), userId);
        redisTemplate.opsForZSet().remove(activeQueueKey(showId), userId);

        // 발급된 Queue Token 관련 키도 함께 삭제
        String token = redisTemplate.opsForValue().get(userTokenKey(showId, userId));
        if (StringUtils.hasText(token)) {
            redisTemplate.delete(tokenKey(token));
        }
        redisTemplate.delete(userTokenKey(showId, userId));
    }

    public void completeAdmission(Long showId, String userId, String queueToken) {
        if (queueTokenBypass || !StringUtils.hasText(queueToken)) {
            return;
        }

        String userTokenKey = userTokenKey(showId, userId);
        String currentToken = redisTemplate.opsForValue().get(userTokenKey);
        if (!queueToken.equals(currentToken)) {
            log.warn("예매 확정 후 Queue Token 정리 건너뜀 - 현재 토큰과 메시지 토큰 불일치. showId={}, userId={}", showId, userId);
            return;
        }

        // 예매 확정(CONFIRMED) 이후에는 같은 입장 권한으로 추가 예매를 시도할 수 없도록 ACTIVE와 토큰 매핑을 정리
        redisTemplate.opsForZSet().remove(activeQueueKey(showId), userId);
        redisTemplate.delete(tokenKey(queueToken));
        redisTemplate.delete(userTokenKey);
    }

    public void validateQueueToken(String token, Long showId, String userId) {
        // ⚠️ 부하테스트(A) 모드: 큐토큰 검증을 통째로 건너뜀.
        //    인터셉터/서비스 양쪽이 이 메서드를 타므로 여기 한 곳이면 둘 다 우회됨.
        if (queueTokenBypass) {
            return;
        }

        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.QUEUE_TOKEN_REQUIRED);
        }

        cleanupExpiredActiveUsers(showId);

        String savedTokenValue = redisTemplate.opsForValue().get(tokenKey(token));
        if (savedTokenValue == null) {
            throw new BusinessException(ErrorCode.QUEUE_TOKEN_EXPIRED);
        }

        String userToken = redisTemplate.opsForValue().get(userTokenKey(showId, userId));
        boolean tokenMismatch = !token.equals(userToken);
        boolean ownerMismatch = !tokenValue(showId, userId).equals(savedTokenValue);
        boolean inactiveUser = !isActiveUser(showId, userId);
        if (tokenMismatch || ownerMismatch || inactiveUser) {
            throw new BusinessException(ErrorCode.INVALID_QUEUE_TOKEN);
        }
    }

    @Scheduled(fixedRateString = "${queue.promotion-interval-ms:1000}")
    public synchronized void promoteQueues() {
        for (Long showId : findQueueShowIds()) {
            cleanupExpiredActiveUsers(showId);
            promoteWaitingUsers(showId);
        }
    }

    private String waitingQueueKey(Long showId) {
        // 공연 단위로 대기열을 분리
        return WAITING_QUEUE_KEY_PREFIX + showId;
    }

    private boolean isActiveUser(Long showId, String userId) {
        return redisTemplate.opsForZSet().score(activeQueueKey(showId), userId) != null;
    }

    private boolean canEnterImmediately(Long showId) {
        return zSetSize(waitingQueueKey(showId)) == 0 && zSetSize(activeQueueKey(showId)) < maxActiveUsers;
    }

    private void cleanupExpiredActiveUsers(Long showId) {
        redisTemplate.opsForZSet().removeRangeByScore(
                activeQueueKey(showId),
                Double.NEGATIVE_INFINITY,
                Instant.now().toEpochMilli()
        );
    }

    private void promoteWaitingUsers(Long showId) {
        if (maxActiveUsers <= 0 || promotionBatchSize <= 0) {
            return;
        }

        String activeQueueKey = activeQueueKey(showId);
        String waitingQueueKey = waitingQueueKey(showId);
        long activeCount = zSetSize(activeQueueKey);
        long slots = Math.min(promotionBatchSize, maxActiveUsers - activeCount);

        while (slots > 0) {
            var nextUsers = redisTemplate.opsForZSet().range(waitingQueueKey, 0, 0);
            if (nextUsers == null || nextUsers.isEmpty()) {
                return;
            }

            String nextUserId = nextUsers.iterator().next();
            redisTemplate.opsForZSet().remove(waitingQueueKey, nextUserId);
            redisTemplate.opsForZSet().add(activeQueueKey, nextUserId, activeExpiresAt());
            slots--;
        }
    }

    private Set<Long> findQueueShowIds() {
        Set<Long> showIds = new HashSet<>();
        collectShowIds(showIds, WAITING_QUEUE_KEY_PREFIX);
        collectShowIds(showIds, ACTIVE_QUEUE_KEY_PREFIX);
        return showIds;
    }

    private void collectShowIds(Set<Long> showIds, String keyPrefix) {
        Set<String> keys = redisTemplate.keys(keyPrefix + "*");
        if (keys == null) {
            return;
        }

        for (String key : keys) {
            String showId = key.substring(keyPrefix.length());
            try {
                showIds.add(Long.parseLong(showId));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private long zSetSize(String key) {
        Long size = redisTemplate.opsForZSet().zCard(key);
        return size == null ? 0 : size;
    }

    private double activeExpiresAt() {
        return Instant.now().plus(QUEUE_TOKEN_TTL).toEpochMilli();
    }

    private String issueQueueToken(Long showId, String userId) {
        redisTemplate.opsForZSet().add(activeQueueKey(showId), userId, activeExpiresAt());

        // ACTIVE 상태에서는 기존 Queue Token을 재사용
        String existingToken = redisTemplate.opsForValue().get(userTokenKey(showId, userId));
        if (StringUtils.hasText(existingToken)) {
            return existingToken;
        }

        // ACTIVE 전환 시 최초 1회만 Queue Token 발급
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(userTokenKey(showId, userId), token, QUEUE_TOKEN_TTL);
        redisTemplate.opsForValue().set(tokenKey(token), tokenValue(showId, userId), QUEUE_TOKEN_TTL);
        return token;
    }

    private String activeQueueKey(Long showId) {
        return ACTIVE_QUEUE_KEY_PREFIX + showId;
    }

    private String userTokenKey(Long showId, String userId) {
        return USER_TOKEN_KEY_PREFIX + showId + ":" + userId;
    }

    private String tokenKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }

    private String tokenValue(Long showId, String userId) {
        return showId + ":" + userId;
    }

    public record QueueStatusResponse(String status, Long position, boolean canEnter, String queueToken) {}
}
