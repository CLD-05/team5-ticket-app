package com.example.ticketing.booking.service;

import com.example.ticketing.booking.sqs.BookingMessage;
import com.example.ticketing.global.exception.ConflictException;
import com.example.ticketing.global.exception.ErrorCode;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final StringRedisTemplate redisTemplate;
    private final SqsTemplate sqsTemplate;
    // Queue Token 검증 적용 시 추가
    // private final SeatRepository seatRepository;
    // private final QueueService queueService;

    public BookingAcceptResponse requestBooking(Long seatId, String userId) {
        // Queue Token 검증 적용 시 메서드 인자에 String queueToken 추가
        // Long showId = seatRepository.findShowIdBySeatId(seatId)
        //         .orElseThrow(() -> new NotFoundException("좌석이 존재하지 않습니다."));
        // queueService.validateQueueToken(queueToken, showId, userId);

        String holdKey = "seat:" + seatId;
        String requestId = UUID.randomUUID().toString();

        // Redis Lua Script: HOLD 여부 확인 및 즉시 삭제(Settle)
        String lua = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                redis.call('DEL', KEYS[1])
                redis.call('SET', 'sold:' .. ARGV[2], ARGV[1])
                return 1
            else
                return 0
            end
            """;

        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(lua, Long.class),
                List.of(holdKey),
                userId,
                String.valueOf(seatId)
        );

        if (result == null || result == 0) {
            throw new ConflictException(ErrorCode.HOLD_EXPIRED);
        }

        // SQS Message 발행 (비동기 처리 요청)
        sqsTemplate.send("booking-queue", new BookingMessage(requestId, seatId, userId));

        return new BookingAcceptResponse(requestId, "ACCEPTED");
    }
    
    public BookingStatusResponse getBookingStatus(String requestId) {
    	String status = redisTemplate.opsForValue().get("result : " + requestId);
    	if (status == null) {
    		return new BookingStatusResponse(requestId, "처리중");
    	}
        return new BookingStatusResponse(requestId, status);
    }

    public record BookingAcceptResponse(String requestId, String status) {}
    public record BookingStatusResponse(String requestId, String status) {}
}
