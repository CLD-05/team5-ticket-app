package com.example.ticketing.booking.worker;

import com.example.ticketing.booking.sqs.BookingMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookingWorker {
    private final BookingProcessor bookingProcessor;
    private final StringRedisTemplate redisTemplate;
    private static final Duration RESULT_TTL = Duration.ofMinutes(5);

    @SqsListener("booking-queue")
    public void confirmBooking(BookingMessage message) {
        log.info("예매 확정 처리 시작: {}, 사용자: {}", message.seatId(), message.userId());

        String result;
        try {
            result = bookingProcessor.process(message);
        } catch (DataIntegrityViolationException e) {
            // Unique 최후 방어선 — 동시 요청 중 패배자. 트랜잭션은 이미 롤백됨.
            result = "예매 실패";
            log.warn("이미 예매된 좌석입니다. - 좌석 : {}", message.seatId());
        }

        saveResult(message.requestId(), result);
    }

    private void saveResult(String requestId, String status) {
        redisTemplate.opsForValue().set("result:" + requestId, status, RESULT_TTL);
    }
}
