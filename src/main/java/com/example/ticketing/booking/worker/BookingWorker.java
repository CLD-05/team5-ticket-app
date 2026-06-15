package com.example.ticketing.booking.worker;

import com.example.ticketing.booking.sqs.BookingMessage;
import com.example.ticketing.queue.service.QueueService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("worker")
@Slf4j
@RequiredArgsConstructor
public class BookingWorker {
    private final BookingProcessor bookingProcessor;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;
    private final QueueService queueService;
    private static final Duration RESULT_TTL = Duration.ofMinutes(5);

    @SqsListener("${app.sqs.booking-queue:booking-queue}")
    public void confirmBooking(BookingMessage message) {
        log.info("예매 확정 처리 시작: {}, 사용자: {}", message.seatId(), message.userId());

        BookingResult outcome;
        try {
            outcome = bookingProcessor.process(message);   // CONFIRMED 또는 FAILED
        } catch (DataIntegrityViolationException e) {
            // Unique 최후 방어선 — 동시 요청 중 패배자. 트랜잭션은 이미 롤백됨. (정상적 거절)
            outcome = BookingResult.CONFLICT;
            log.warn("이미 예매된 좌석입니다. - 좌석 : {}", message.seatId());
        }

        // 확정 결과 카운터 → booking_confirm_total{result="confirmed|conflict|failed"} (SLO용)
        meterRegistry.counter("booking.confirm", "result", outcome.metricLabel()).increment();

        // e2e 확정 시간 → booking_confirm_e2e_seconds (큐 인입~확정, 진짜 SLI)
        long elapsedMs = System.currentTimeMillis() - message.createdAtEpochMs();
        meterRegistry.timer("booking.confirm.e2e").record(elapsedMs, TimeUnit.MILLISECONDS);

        // status 응답/폴링용 코드값 저장 (CONFIRMED/CONFLICT/FAILED)
        saveResult(message.requestId(), outcome.name());

        if (outcome == BookingResult.CONFIRMED) {
            // 최종 예매 성공 시점에만 Queue Token을 정리한다.
            // CONFLICT/FAILED는 사용자가 같은 입장 권한으로 다른 좌석을 다시 시도할 수 있도록 유지한다.
            queueService.completeAdmission(message.showId(), message.userId(), message.queueToken());
        }
    }

    private void saveResult(String requestId, String status) {
        redisTemplate.opsForValue().set("result:" + requestId, status, RESULT_TTL);
    }
}
