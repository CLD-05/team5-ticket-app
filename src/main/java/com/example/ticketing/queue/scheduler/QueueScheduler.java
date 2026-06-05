package com.example.ticketing.queue.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class QueueScheduler {

    private final StringRedisTemplate redisTemplate;
    private static final String QUEUE_KEY = "ticketing:queue";
    private static final String SCHEDULER_LOCK_KEY = "lock:queue:scheduler";

    @Value("${queue.process-size:100}")
    private int processSize;

    @Scheduled(fixedDelayString = "${queue.interval-ms:5000}")
    public void processQueue() {
        // Distributed lock to prevent parallel execution across instances
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(SCHEDULER_LOCK_KEY, "locked", Duration.ofSeconds(4));
        if (!Boolean.TRUE.equals(acquired)) {
            return;
        }

        try {
            Set<String> users = redisTemplate.opsForZSet().range(QUEUE_KEY, 0, processSize - 1);
            if (users == null || users.isEmpty()) {
                return;
            }

            log.info("Processing {} users from the waiting queue", users.size());

            for (String userId : users) {
                String token = UUID.randomUUID().toString();
                // User can enter, token is valid for 10 minutes
                redisTemplate.opsForValue().set("queue:token:" + userId, token, Duration.ofMinutes(10));
                redisTemplate.opsForZSet().remove(QUEUE_KEY, userId);
            }
        } finally {
            redisTemplate.delete(SCHEDULER_LOCK_KEY);
        }
    }
}
