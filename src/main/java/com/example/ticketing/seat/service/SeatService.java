package com.example.ticketing.seat.service;

import com.example.ticketing.global.exception.BusinessException;
import com.example.ticketing.global.exception.ConflictException;
import com.example.ticketing.global.exception.ErrorCode;
import com.example.ticketing.global.exception.NotFoundException;
import com.example.ticketing.seat.dto.SeatResponse;
import com.example.ticketing.seat.entity.Seat;
import com.example.ticketing.seat.entity.SeatStatus;
import com.example.ticketing.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {
    private final StringRedisTemplate redisTemplate;
    private final SeatRepository seatRepository;
    private static final Duration HOLD_TTL = Duration.ofMinutes(5);

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsForShow(Long showId) {
        List<Seat> seats = seatRepository.findByShowId(showId);
        if (seats.isEmpty()) {
            return List.of();
        }

        List<String> redisKeys = seats.stream()
                .map(seat -> "seat:" + seat.getId())
                .toList();

        List<String> heldUsers = redisTemplate.opsForValue().multiGet(redisKeys);

        List<SeatResponse> responses = new ArrayList<>();
        for (int i = 0; i < seats.size(); i++) {
            Seat seat = seats.get(i);
            SeatStatus status = seat.getStatus();

            if (status != SeatStatus.SOLD) {
                String heldUser = heldUsers != null && i < heldUsers.size() ? heldUsers.get(i) : null;
                if (heldUser != null) {
                    status = SeatStatus.HOLD;
                }
            }

            responses.add(new SeatResponse(seat.getId(), seat.getSeatNumber(), seat.getPrice(), status));
        }

        return responses;
    }

    public SeatHoldResponse holdSeat(Long seatId, String userId) {
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

    public void releaseSeat(Long seatId, String userId) {
        String key = "seat:" + seatId;
        String heldBy = redisTemplate.opsForValue().get(key);
        if (heldBy != null) {
            if (userId.equals(heldBy)) {
                redisTemplate.delete(key);
            } else {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }
        }
    }

    @Transactional
    public void releaseSeatInDbIfAvailable(Long seatId) {
        seatRepository.findById(seatId).ifPresent(seat -> {
            if (seat.getStatus() != SeatStatus.SOLD) {
                seat.available();
                seatRepository.save(seat);
            }
        });
    }

    public record SeatHoldResponse(Long seatId, int ttlSeconds) {}
}
