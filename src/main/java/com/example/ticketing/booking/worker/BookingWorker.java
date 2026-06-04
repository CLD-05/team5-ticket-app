package com.example.ticketing.booking.worker;

import com.example.ticketing.booking.entity.Booking;
import com.example.ticketing.booking.repository.BookingRepository;
import com.example.ticketing.booking.sqs.BookingMessage;
import com.example.ticketing.seat.entity.Seat;
import com.example.ticketing.seat.repository.SeatRepository;
import com.example.ticketing.user.entity.User;
import com.example.ticketing.user.repository.UserRepository;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookingWorker {
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @SqsListener("booking-queue")
    @Transactional
    public void confirmBooking(BookingMessage message) {
        log.info("Processing booking for seat: {}, user: {}", message.seatId(), message.userId());

        // 1. 비관적 락 획득 (SELECT ... FOR UPDATE)
        Seat seat = seatRepository.findByIdWithLock(message.seatId())
                .orElseThrow(() -> new RuntimeException("좌석을 찾을 수 없습니다."));

        if (seat.getStatus() == com.example.ticketing.seat.entity.SeatStatus.SOLD) {
            log.warn("Seat {} already sold, skipping.", seat.getId());
            return;
        }

        User user = userRepository.findById(message.userId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 2. 상태 변경 (SOLD)
        seat.sold();

        // 3. 예매 내역 저장 (Unique 제약조건으로 최종 방어)
        bookingRepository.save(new Booking(seat, user));

        log.info("Booking confirmed for seat: {}", seat.getId());
    }
}
