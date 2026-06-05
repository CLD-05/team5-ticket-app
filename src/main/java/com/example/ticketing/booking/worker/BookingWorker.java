package com.example.ticketing.booking.worker;

import com.example.ticketing.booking.entity.Booking;
import com.example.ticketing.booking.repository.BookingRepository;
import com.example.ticketing.booking.sqs.BookingMessage;
import com.example.ticketing.seat.entity.Seat;
import com.example.ticketing.seat.entity.SeatStatus;
import com.example.ticketing.seat.repository.SeatRepository;
import com.example.ticketing.user.entity.User;
import com.example.ticketing.user.repository.UserRepository;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookingWorker {
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final StringRedisTemplate redisTemplate;
    private static final Duration RESULT_TTL = Duration.ofMinutes(5);

    @SqsListener("booking-queue")
    @Transactional
    public void confirmBooking(BookingMessage message) {
        log.info("예매 확정 처리 시작: {}, 사용자: {}", message.seatId(), message.userId());

        // 1. 좌석 조회
        Seat seat = seatRepository.findById(message.seatId()).orElse(null);
        
        if (seat == null) {
        	log.error("좌석을 찾을 수 없습니다. - 좌석 : {}", message.seatId());
        	saveResult(message.requestId(), "예매 실패");
        	return; 
        }


        if (seat.getStatus() == SeatStatus.SOLD) {
            log.warn("이미 판매 완료된 좌석입니다. - 좌석 : {}", seat.getId());
            saveResult(message.requestId(), "예매 성공");
            return;
        }

        User user = userRepository.findById(message.userId()).orElse(null);
        if (user == null) {
			log.error("사용자를 찾을 수 없습니다. 사용자 : {}", message.userId());
			saveResult(message.requestId(), "예매 실패");
			return;
        }
        // 2. 상태 변경 (SOLD)
        try {
        	seat.sold();
            // 3. 예매 내역 저장 (Unique 제약조건으로 최종 방어)
        	bookingRepository.save(new Booking(seat, user));
        	saveResult(message.requestId(), "예매 성공");
        	log.info("예매 확정 완료 - 좌석: {}", seat.getId());
        } catch (DataIntegrityViolationException e) {
        	// 데이터 무결성 제약 조건
        	saveResult(message.requestId(), "예매 실패");
        	log.warn("이미 예매된 좌석입니다. - 좌석 : {}", message.seatId());
        }
    }

	private void saveResult(String requestId, String status) {
		redisTemplate.opsForValue().set("result : " + requestId, status, RESULT_TTL);
		
	}
}
