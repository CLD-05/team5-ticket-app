package com.example.ticketing.queue.service;

import com.example.ticketing.global.exception.BusinessException;
import com.example.ticketing.global.exception.NotFoundException;
import com.example.ticketing.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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

    @Value("${queue.estimated-admission-rate-per-minute:100}")
    private long estimatedAdmissionRatePerMinute;

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
        checkBookingOpenTime(showId);
        double score = Instant.now().toEpochMilli();

        // 같은 사용자가 다시 진입하면 score를 갱신해 대기열 뒤로 보냄
        redisTemplate.opsForZSet().add(waitingQueueKey(showId), userId, score);
        return "JOINED";
    }

    /**
     * enter 진입 판정을 Redis Lua로 원자화한 스크립트.
     * Redis는 단일 스레드라 스크립트 실행 중 다른 명령이 끼어들 수 없으므로,
     * 기존 synchronized(JVM 락)로 보호하던 check-then-act(여유 확인 → ZADD)를
     * 락 없이 정합성 있게 처리한다.
     *
     * KEYS[1] = active ZSET, KEYS[2] = waiting ZSET
     * ARGV[1] = userId, ARGV[2] = now(ms), ARGV[3] = activeExpiresAt(ms), ARGV[4] = maxActiveUsers
     *
     * 반환: { code, rank }
     *   code 0 = 이미 ACTIVE
     *   code 1 = 즉시 입장 가능(여유 있음) — 실제 active ZADD는 issueQueueToken이 수행
     *   code 2 = WAITING (rank = 1-based 순번)
     *
     * active 등록(ZADD)은 ENTER_LUA가 원자적으로 수행(동시성 정합성 핵심).
     * issueQueueToken의 ZADD는 기존 active의 TTL 갱신용(멱등).
     */
    private static final String ENTER_LUA =
            // 만료된 active 정리 (score < now 제거)
            "redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[2]) " +
            // 카운트를 먼저 구해 모든 분기에서 함께 반환 (Java의 별도 ZCARD 왕복 제거)
            "local activeCnt = redis.call('ZCARD', KEYS[1]) " +
            "local waitingCnt = redis.call('ZCARD', KEYS[2]) " +
            // 이미 active면 code 0
            "if redis.call('ZSCORE', KEYS[1], ARGV[1]) then return {0, -1, waitingCnt, activeCnt} end " +
            // 이미 waiting이면 현재 순번
            "local wr = redis.call('ZRANK', KEYS[2], ARGV[1]) " +
            "if wr then return {2, wr + 1, waitingCnt, activeCnt} end " +
            // 대기자 0명 && active 여유 있으면 즉시 입장 (여유 확인+ZADD 원자적)
            "if waitingCnt == 0 and activeCnt < tonumber(ARGV[4]) then " +
            "  redis.call('ZADD', KEYS[1], ARGV[3], ARGV[1]) " +
            "  return {1, -1, waitingCnt, activeCnt + 1} " +
            "end " +
            // 그 외 → waiting 추가 후 순번 반환
            "redis.call('ZADD', KEYS[2], ARGV[2], ARGV[1]) " +
            "local nr = redis.call('ZRANK', KEYS[2], ARGV[1]) " +
            "return {2, nr + 1, waitingCnt + 1, activeCnt}";

    /**
     * enterQueue: synchronized 제거. 진입 판정은 ENTER_LUA가 원자적으로 수행한다.
     * 파드당 직렬화 병목(JVM 단일 락)이 사라지고, promoteQueues와도 락을 공유하지 않는다.
     */
    public QueueStatusResponse enterQueue(Long showId, String userId) {
        checkBookingOpenTime(showId);

        @SuppressWarnings("unchecked")
        java.util.List<Long> result = redisTemplate.execute(
                new DefaultRedisScript<>(ENTER_LUA, java.util.List.class),
                java.util.List.of(activeQueueKey(showId), waitingQueueKey(showId)),
                userId,
                String.valueOf(Instant.now().toEpochMilli()),
                String.valueOf(activeExpiresAt()),
                String.valueOf(maxActiveUsers)
        );

        long code = (result != null && !result.isEmpty()) ? result.get(0) : 2L;
        // Lua가 계산한 카운트를 재사용 → 별도 ZCARD 왕복 제거
        long waitingCnt = (result != null && result.size() > 2) ? result.get(2) : 0L;
        long activeCnt  = (result != null && result.size() > 3) ? result.get(3) : 0L;
        QueueCounts counts = new QueueCounts(waitingCnt, activeCnt);

        if (code == 0L || code == 1L) {
            // ACTIVE (기존이거나 신규 승급) → 토큰 발급
            return activeResponse(showId, userId, counts);
        }
        // WAITING
        Long rank = (result != null && result.size() > 1) ? result.get(1) : null;
        return waitingResponse(showId, rank, counts);
    }

    /**
     * getStatus: 읽기 전용(rank 조회)이라 락이 불필요. cleanup만 원자적으로 처리.
     */
    public QueueStatusResponse getStatus(Long showId, String userId) {
        cleanupExpiredActiveUsers(showId);

        // Redis ZSET rank는 0부터 시작하므로 응답에서는 1을 더함
        Long rank = redisTemplate.opsForZSet().rank(waitingQueueKey(showId), userId);
        QueueCounts counts = queueCounts(showId);
        if (rank == null && isActiveUser(showId, userId)) {
            return activeResponse(showId, userId, counts);
        }

        if (rank == null) {
            throw new NotFoundException(ErrorCode.QUEUE_NOT_FOUND);
        }

        return waitingResponse(showId, rank + 1, counts);
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

    // promote 전용 락. enterQueue/getStatus와 'this' 락을 공유하지 않아
    // 1초 주기 promote가 enter 진입을 막던 톱니 간섭을 제거한다.
    // (promote끼리의 중복 실행만 막으면 충분 — enter는 ENTER_LUA가 원자성 보장)
    private final Object promoteLock = new Object();

    @Scheduled(fixedRateString = "${queue.promotion-interval-ms:1000}")
    public void promoteQueues() {
        synchronized (promoteLock) {
            for (Long showId : findQueueShowIds()) {
                cleanupExpiredActiveUsers(showId);
                promoteWaitingUsers(showId);
            }
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

    private QueueStatusResponse activeResponse(Long showId, String userId, QueueCounts counts) {
        // counts는 ENTER_LUA가 이미 계산한 값을 재사용 (별도 ZCARD 왕복 없음)
        return new QueueStatusResponse("ACTIVE", null, true, issueQueueToken(showId, userId),
                counts.waitingCount(), counts.activeCount(), counts.totalCount(), null);
    }

    private QueueStatusResponse waitingResponse(Long showId, Long position, QueueCounts counts) {
        // counts는 ENTER_LUA가 이미 계산한 값을 재사용 (별도 ZCARD 왕복 없음)
        return new QueueStatusResponse("WAITING", position, false, null,
                counts.waitingCount(), counts.activeCount(), counts.totalCount(), estimateWaitSeconds(position));
    }

    private QueueCounts queueCounts(Long showId) {
        long waitingCount = zSetSize(waitingQueueKey(showId));
        long activeCount = zSetSize(activeQueueKey(showId));
        return new QueueCounts(waitingCount, activeCount);
    }

    private Long estimateWaitSeconds(Long position) {
        if (position == null || estimatedAdmissionRatePerMinute <= 0) {
            return null;
        }
        return Math.max(1, (long) Math.ceil(position * 60.0 / estimatedAdmissionRatePerMinute));
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

    public void clearQueue(Long showId) {
        log.info("Queue clearing triggered for showId: {}", showId);
        String waitingKey = waitingQueueKey(showId);
        String activeKey = activeQueueKey(showId);

        String userTokenPattern = USER_TOKEN_KEY_PREFIX + showId + ":*";
        Set<String> userTokenKeys = redisTemplate.keys(userTokenPattern);
        if (userTokenKeys != null && !userTokenKeys.isEmpty()) {
            Set<String> tokenKeysToDelete = new HashSet<>();
            for (String userTokKey : userTokenKeys) {
                String token = redisTemplate.opsForValue().get(userTokKey);
                if (StringUtils.hasText(token)) {
                    tokenKeysToDelete.add(tokenKey(token));
                }
            }
            if (!tokenKeysToDelete.isEmpty()) {
                redisTemplate.delete(tokenKeysToDelete);
            }
            redisTemplate.delete(userTokenKeys);
        }

        redisTemplate.delete(waitingKey);
        redisTemplate.delete(activeKey);
        log.info("Queue cleared for showId: {}", showId);
    }

    public void clearAllQueues() {
        log.info("Queue clearing triggered for all shows");
        Set<String> keys = redisTemplate.keys("queue:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        log.info("All queues cleared");
    }

    private String tokenValue(Long showId, String userId) {
        return showId + ":" + userId;
    }

    private void checkBookingOpenTime(Long showId) {
        String key = "show:" + showId + ":booking_open_at";
        String val = redisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(val)) {
            try {
                long openTime = Long.parseLong(val);
                long now = Instant.now().getEpochSecond();
                if (now < openTime) {
                    throw new BusinessException(ErrorCode.BOOKING_NOT_OPEN);
                }
            } catch (NumberFormatException e) {
                // Ignore parse errors
            }
        }
    }

    @Schema(description = "대기열 상태 응답")
    public record QueueStatusResponse(
            @Schema(description = "대기열 상태", example = "WAITING", allowableValues = {"WAITING", "ACTIVE"})
            String status,
            @Schema(description = "대기 순번. ACTIVE 상태에서는 null입니다.", example = "37")
            Long position,
            @Schema(description = "예매 화면 입장 가능 여부", example = "false")
            boolean canEnter,
            @Schema(description = "예매 요청에 사용할 Queue Token. WAITING 상태에서는 null입니다.", example = "550e8400-e29b-41d4-a716-446655440000")
            String queueToken,
            @Schema(description = "현재 대기 중인 사용자 수", example = "1200")
            long waitingCount,
            @Schema(description = "현재 입장 가능 상태인 사용자 수", example = "300")
            long activeCount,
            @Schema(description = "현재 대기열 전체 인원 수", example = "1500")
            long totalQueueCount,
            @Schema(description = "예상 대기 시간(초). ACTIVE 상태에서는 null입니다.", example = "180")
            Long estimatedWaitSeconds
    ) {}

    private record QueueCounts(long waitingCount, long activeCount) {
        long totalCount() {
            return waitingCount + activeCount;
        }
    }
}