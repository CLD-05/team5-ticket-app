package com.example.ticketing.queue.service;

import com.example.ticketing.global.exception.NotFoundException;
import com.example.ticketing.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class QueueService {
    private final StringRedisTemplate redisTemplate;
    private static final String QUEUE_KEY = "ticketing:queue";

    public String joinQueue(String userId) {
        double score = Instant.now().toEpochMilli();
        redisTemplate.opsForZSet().add(QUEUE_KEY, userId, score);
        return "JOINED";
    }

    public QueueStatusResponse getStatus(String userId) {
        Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY, userId);
        if (rank == null) {
            throw new NotFoundException(ErrorCode.QUEUE_NOT_FOUND);
        }

        return new QueueStatusResponse("WAITING", rank + 1);
    }

    public void leaveQueue(String userId) {
        redisTemplate.opsForZSet().remove(QUEUE_KEY, userId);
    }

    public record QueueStatusResponse(String status, Long position) {}
}
