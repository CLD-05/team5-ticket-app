package com.example.ticketing.booking.worker;

import com.example.ticketing.booking.sqs.BookingMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.TimeUnit;   // 추가

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookingWorker {
    private final BookingProcessor bookingProcessor;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;   // 추가
    private static final Duration RESULT_TTL = Duration.ofMinutes(5);

    @SqsListener("booking-queue")
    public void confirmBooking(BookingMessage message) {
        log.info("예매 확정 처리 시작: {}, 사용자: {}", message.seatId(), message.userId());

        String result;
        String outcome;  // 메트릭 태그용
        try {
            result = bookingProcessor.process(message);
            outcome = "예매 성공".equals(result) ? "confiremd" : "failed";
           } catch (DataIntegrityViolationException e) {
            // Unique 최후 방어선 — 동시 요청 중 패배자. 트랜잭션은 이미 롤백됨.
            result = "예매 실패";
            outcome = "conflict";
            log.warn("이미 예매된 좌석입니다. - 좌석 : {}", message.seatId());
        }
        // 확정 결과 카운터 → booking_confirm_total{result="..."} (SLO 알림용)
        meterRegistry.counter("booking.confirm", "result", outcome).increment();
        
        // e2e 확정 시간 → booking_confirm_e2e_seconds (큐 인입~확정, 진짜 SLI)
        long elapsedMs = System.currentTimeMillis() - message.createdAtEpochMs();
        meterRegistry.timer("booking.confirm.e2e").record(elapsedMs, TimeUnit.MILLISECONDS);
        
        saveResult(message.requestId(), result);
    }

    private void saveResult(String requestId, String status) {
        redisTemplate.opsForValue().set("result:" + requestId, status, RESULT_TTL);
    }
}
