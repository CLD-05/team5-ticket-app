package com.example.ticketing.seat.service;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SeatLockService {

    private final RedissonClient redissonClient;
    private static final String LOCK_PREFIX = "lock:seat:";

    public boolean acquireLock(Long seatId) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + seatId);
        try {
            return lock.tryLock(0, 3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void releaseLock(Long seatId) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + seatId);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}