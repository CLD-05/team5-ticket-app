package com.example.ticketing.queue.service;

import com.example.ticketing.global.exception.BusinessException;
import com.example.ticketing.global.exception.NotFoundException;
import com.example.ticketing.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueService {
    private final StringRedisTemplate redisTemplate;
    private static final String WAITING_QUEUE_KEY_PREFIX = "queue:waiting:";
    private static final String ACTIVE_USER_KEY_PREFIX = "queue:active:";
    private static final String USER_TOKEN_KEY_PREFIX = "queue:user-token:";
    private static final String TOKEN_KEY_PREFIX = "queue:token:";
    private static final Duration QUEUE_TOKEN_TTL = Duration.ofMinutes(10);

    @Value("${queue.active-entry-limit:100}")
    private int activeEntryLimit;

    public String joinQueue(Long showId, String userId) {
        double score = Instant.now().toEpochMilli();

        // 같은 사용자가 다시 진입하면 score를 갱신해 대기열 뒤로 보냄
        redisTemplate.opsForZSet().add(waitingQueueKey(showId), userId, score);
        return "JOINED";
    }

    public QueueStatusResponse getStatus(Long showId, String userId) {
        // Redis ZSET rank는 0부터 시작하므로 응답에서는 1을 더함
        Long rank = redisTemplate.opsForZSet().rank(waitingQueueKey(showId), userId);
        if (rank == null && isActiveUser(showId, userId)) {
            return new QueueStatusResponse("ACTIVE", null, true, issueQueueToken(showId, userId));
        }

        if (rank == null) {
            throw new NotFoundException(ErrorCode.QUEUE_NOT_FOUND);
        }

        if (rank < activeEntryLimit) {
            // 입장 허용 사용자는 waiting에서 제거하고 active 상태로 전환
            redisTemplate.opsForZSet().remove(waitingQueueKey(showId), userId);
            redisTemplate.opsForValue().set(activeUserKey(showId, userId), "ACTIVE", QUEUE_TOKEN_TTL);

            return new QueueStatusResponse("ACTIVE", null, true, issueQueueToken(showId, userId));
        }

        return new QueueStatusResponse("WAITING", rank + 1, false, null);
    }

    public void leaveQueue(Long showId, String userId) {
        // 사용자를 해당 공연의 대기열 ZSET에서 제거
        redisTemplate.opsForZSet().remove(waitingQueueKey(showId), userId);
        redisTemplate.delete(activeUserKey(showId, userId));

        // 발급된 Queue Token 관련 키도 함께 삭제
        String token = redisTemplate.opsForValue().get(userTokenKey(showId, userId));
        if (StringUtils.hasText(token)) {
            redisTemplate.delete(tokenKey(token));
        }
        redisTemplate.delete(userTokenKey(showId, userId));
    }

    public void validateQueueToken(String token, Long showId, String userId) {
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.QUEUE_TOKEN_REQUIRED);
        }

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

    private String waitingQueueKey(Long showId) {
        // 공연 단위로 대기열을 분리
        return WAITING_QUEUE_KEY_PREFIX + showId;
    }

    private boolean isActiveUser(Long showId, String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(activeUserKey(showId, userId)));
    }

    private String issueQueueToken(Long showId, String userId) {
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

    private String activeUserKey(Long showId, String userId) {
        return ACTIVE_USER_KEY_PREFIX + showId + ":" + userId;
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
