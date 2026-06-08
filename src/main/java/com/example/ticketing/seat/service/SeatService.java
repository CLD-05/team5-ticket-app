package com.example.ticketing.seat.service;

import com.example.ticketing.global.exception.ConflictException;
import com.example.ticketing.global.exception.ErrorCode;
import com.example.ticketing.global.exception.NotFoundException;
import com.example.ticketing.seat.dto.SeatResponseDto;
import com.example.ticketing.seat.entity.Seat;
import com.example.ticketing.seat.entity.SeatStatus;
import com.example.ticketing.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatService {
    private final StringRedisTemplate redisTemplate;
    private final SeatRepository seatRepository;
    // Queue Token 검증 적용 시 추가
    // private final QueueService queueService;

    @Value("${app.seat.hold-ttl-seconds:300}")
    private long holdTtlSeconds;

    private Duration getHoldTtl() {
        return Duration.ofSeconds(holdTtlSeconds);
    }

    @Transactional(readOnly = true)
    public List<SeatResponseDto> getSeats(Long showId) {
        return seatRepository.findByShowId(showId).stream().map(seat -> {
            SeatStatus status = seat.getStatus();
            if (status == SeatStatus.AVAILABLE && Boolean.TRUE.equals(redisTemplate.hasKey("seat:" + seat.getId()))) {
                status = SeatStatus.HOLD;
            }
            return new SeatResponseDto(
                    seat.getId(),
                    showId,
                    seat.getSeatNumber(),
                    seat.getPrice(),
                    status
            );
        }).collect(Collectors.toList());
    }

    public SeatHoldResponse holdSeat(Long seatId, String userId) {
        // Queue Token 검증 적용 시 메서드 인자에 String queueToken 추가
        // Long showId = seatRepository.findShowIdBySeatId(seatId)
        //         .orElseThrow(() -> new NotFoundException("좌석이 존재하지 않습니다."));
        // queueService.validateQueueToken(queueToken, showId, userId);

        String key = "seat:" + seatId;

        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(key, userId, getHoldTtl());

        if (!Boolean.TRUE.equals(success)) {
            throw new ConflictException(ErrorCode.SEAT_ALREADY_HELD);
        }

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new NotFoundException("좌석이 존재하지 않습니다."));

        if (seat.getStatus() == SeatStatus.SOLD) {
            redisTemplate.delete(key);
            throw new ConflictException(ErrorCode.SEAT_ALREADY_SOLD);
        }

        return new SeatHoldResponse(seatId, (int) holdTtlSeconds);
    }

    public boolean checkSeatHolder(Long seatId, String userId) {
        String key = "seat:" + seatId;
        String holdingUserId = redisTemplate.opsForValue().get(key);
        return userId.equals(holdingUserId);
    }

    public void releaseSeat(Long seatId, String userId) {
        String key = "seat:" + seatId;
        String holdingUserId = redisTemplate.opsForValue().get(key);

        if (userId.equals(holdingUserId)) {
            redisTemplate.delete(key);
        }
    }

    public record SeatHoldResponse(Long seatId, int ttlSeconds) {}
}
