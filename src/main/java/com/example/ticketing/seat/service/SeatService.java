package com.example.ticketing.seat.service;

import com.example.ticketing.global.exception.ConflictException;
import com.example.ticketing.global.exception.ErrorCode;
import com.example.ticketing.global.exception.NotFoundException;
import com.example.ticketing.seat.entity.Seat;
import com.example.ticketing.seat.entity.SeatStatus;
import com.example.ticketing.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class SeatService {
    private final StringRedisTemplate redisTemplate;
    private final SeatRepository seatRepository;
    // Queue Token 검증 적용 시 추가
    // private final QueueService queueService;
    private static final Duration HOLD_TTL = Duration.ofMinutes(5);

    public SeatHoldResponse holdSeat(Long seatId, String userId) {
        // Queue Token 검증 적용 시 메서드 인자에 String queueToken 추가
        // Long showId = seatRepository.findShowIdBySeatId(seatId)
        //         .orElseThrow(() -> new NotFoundException("좌석이 존재하지 않습니다."));
        // queueService.validateQueueToken(queueToken, showId, userId);

        String key = "seat:" + seatId;

        // 1. Redis SET NX (Atomic Hold)
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, userId, HOLD_TTL);

        if (!Boolean.TRUE.equals(success)) {
            throw new ConflictException(ErrorCode.SEAT_ALREADY_HELD);
        }

        // 2. DB 상태 확인
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new NotFoundException("좌석이 존재하지 않습니다."));

        if (seat.getStatus() == SeatStatus.SOLD) {
            redisTemplate.delete(key);
            throw new ConflictException(ErrorCode.SEAT_ALREADY_SOLD);
        }

        return new SeatHoldResponse(seatId, 300);
    }

    public record SeatHoldResponse(Long seatId, int ttlSeconds) {}
}
