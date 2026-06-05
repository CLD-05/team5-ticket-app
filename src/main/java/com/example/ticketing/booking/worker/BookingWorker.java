package com.example.ticketing.booking.worker;

import com.example.ticketing.booking.entity.Booking;
import com.example.ticketing.booking.service.BookingService;
import com.example.ticketing.booking.sqs.BookingMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookingWorker {
    private final BookingService bookingService;
    private final StringRedisTemplate redisTemplate;

    @SqsListener("booking-queue")
    public void confirmBooking(BookingMessage message) {
        log.info("Processing booking for seat: {}, user: {}", message.seatId(), message.userId());
        try {
            Booking booking = bookingService.confirmBooking(message);
            // Update status in Redis so polling client gets "SUCCESS" and the booking ID
            redisTemplate.opsForValue().set("booking:status:" + message.requestId(), "SUCCESS:" + booking.getId(), Duration.ofMinutes(30));
            log.info("Booking confirmed for seat: {} and requestId: {}", message.seatId(), message.requestId());
        } catch (Exception e) {
            log.error("Failed to confirm booking for requestId: {}", message.requestId(), e);
            redisTemplate.opsForValue().set("booking:status:" + message.requestId(), "FAILED", Duration.ofMinutes(30));
        }
    }
}
