package com.example.ticketing.queue.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * enter Lua 원자화 검증 테스트 (경량판).
 * @SpringBootTest 미사용 — 앱 전체(SQS/AWS)를 띄우면 부팅 의존성으로 컨텍스트 로딩 실패.
 * enter는 Redis만 필요하므로 QueueService를 직접 new 하고 StringRedisTemplate만 연결한다.
 * 사전조건: localhost:6379 Redis.
 */
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "redis.test", matches = "true")

class QueueServiceEnterLuaTest {

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private QueueService queueService;

    private static final long SHOW_ID = 999L;

    @BeforeAll
    static void setUpRedis() {
        connectionFactory = new LettuceConnectionFactory("localhost", 6379);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUpService() {
        var keys = redisTemplate.keys("queue:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        queueService = new QueueService(redisTemplate);
        ReflectionTestUtils.setField(queueService, "maxActiveUsers", 2);
        ReflectionTestUtils.setField(queueService, "promotionBatchSize", 10);
        ReflectionTestUtils.setField(queueService, "estimatedAdmissionRatePerMinute", 100L);
        ReflectionTestUtils.setField(queueService, "queueTokenBypass", false);
    }

    @Test
    @DisplayName("첫 진입 사용자는 즉시 입장(ACTIVE) + 큐토큰 발급 - 배열 매핑 정상")
    void firstUser_entersImmediately() {
        var res = queueService.enterQueue(SHOW_ID, "user-1");
        assertThat(res.canEnter()).isTrue();
        assertThat(res.status()).isEqualTo("ACTIVE");
        assertThat(res.queueToken()).isNotBlank();
    }

    @Test
    @DisplayName("정원(2)이 차면 WAITING + 순번 부여")
    void overCapacity_goesToWaiting_withRank() {
        assertThat(queueService.enterQueue(SHOW_ID, "user-1").canEnter()).isTrue();
        assertThat(queueService.enterQueue(SHOW_ID, "user-2").canEnter()).isTrue();

        var third = queueService.enterQueue(SHOW_ID, "user-3");
        assertThat(third.canEnter()).isFalse();
        assertThat(third.status()).isEqualTo("WAITING");
        assertThat(third.position()).isEqualTo(1L);

        var fourth = queueService.enterQueue(SHOW_ID, "user-4");
        assertThat(fourth.position()).isEqualTo(2L);
    }

    @Test
    @DisplayName("이미 ACTIVE 사용자 재진입은 멱등 - active 중복 등록 안 함")
    void activeUser_reenter_isIdempotent() {
        queueService.enterQueue(SHOW_ID, "user-1");
        var again = queueService.enterQueue(SHOW_ID, "user-1");

        assertThat(again.canEnter()).isTrue();
        assertThat(again.status()).isEqualTo("ACTIVE");

        Long activeCount = redisTemplate.opsForZSet().zCard("queue:active:" + SHOW_ID);
        assertThat(activeCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("이미 WAITING 사용자 재진입은 순번 유지")
    void waitingUser_reenter_keepsRank() {
        queueService.enterQueue(SHOW_ID, "user-1");
        queueService.enterQueue(SHOW_ID, "user-2");
        var first = queueService.enterQueue(SHOW_ID, "user-3");
        assertThat(first.position()).isEqualTo(1L);

        var again = queueService.enterQueue(SHOW_ID, "user-3");
        assertThat(again.position()).isEqualTo(1L);

        Long waitingCount = redisTemplate.opsForZSet().zCard("queue:waiting:" + SHOW_ID);
        assertThat(waitingCount).isEqualTo(1L);
    }

    @Test
    @DisplayName("동시 진입 50스레드에도 정원 초과/유실 없음 - synchronized 없이 Lua 원자성")
    void concurrentEnter_doesNotExceedCapacity() throws InterruptedException {
        int threads = 50;
        var pool = java.util.concurrent.Executors.newFixedThreadPool(threads);
        var latch = new java.util.concurrent.CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            final String uid = "u" + i;
            pool.submit(() -> {
                try {
                    queueService.enterQueue(SHOW_ID, uid);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();

        Long activeCount = redisTemplate.opsForZSet().zCard("queue:active:" + SHOW_ID);
        Long waitingCount = redisTemplate.opsForZSet().zCard("queue:waiting:" + SHOW_ID);

        assertThat(activeCount).isLessThanOrEqualTo(2L);
        assertThat(activeCount + waitingCount).isEqualTo(50L);
    }
}
