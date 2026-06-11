package com.example.ticketing.booking.service;

import com.example.ticketing.booking.entity.Booking;
import com.example.ticketing.seat.entity.Seat;
import com.example.ticketing.show.repository.SeatGradeRepository;
import com.example.ticketing.booking.sqs.BookingMessage;
import com.example.ticketing.global.exception.ConflictException;
import com.example.ticketing.global.exception.ErrorCode;
import com.example.ticketing.global.exception.NotFoundException;
import com.example.ticketing.queue.service.QueueService;
import com.example.ticketing.seat.repository.SeatRepository;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import com.example.ticketing.booking.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {
    private final StringRedisTemplate redisTemplate;
    private final SqsTemplate sqsTemplate;
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final QueueService queueService;
    private final SeatGradeRepository seatGradeRepository;

    @Value("${app.sqs.booking-queue:booking-queue}")
    private String bookingQueue;

    public BookingAcceptResponse requestBooking(Long seatId, String userId, String queueToken) {
        Long showId = seatRepository.findShowIdBySeatId(seatId)
                .orElseThrow(() -> new NotFoundException("좌석이 존재하지 않습니다."));

        // 서비스 계층에서 Queue Token을 한 번 더 검증
        queueService.validateQueueToken(queueToken, showId, userId);

        String holdKey = "seat:" + seatId;
        String requestId = UUID.randomUUID().toString();

        // Redis Lua Script: HOLD 여부 확인 및 즉시 삭제(Settle)
        String lua = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                redis.call('DEL', KEYS[1])
                redis.call('SET', 'sold:' .. ARGV[2], ARGV[1], 'EX', 600)
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
        sqsTemplate.send(bookingQueue, new BookingMessage(requestId, seatId, userId, System.currentTimeMillis()));

        return new BookingAcceptResponse(requestId, "ACCEPTED");
    }
    
    public BookingStatusResponse getBookingStatus(String requestId) {
    	String status = redisTemplate.opsForValue().get("result:" + requestId);
    	if (status == null) {
    		return new BookingStatusResponse(requestId, "PROCESSING");
    	}
        return new BookingStatusResponse(requestId, status);
    }

    @Transactional(readOnly = true)
    public List<UserBookingResponse> getUserBookings(String userId) {
        return bookingRepository.findByUserIdWithSeatAndShow(userId).stream()
                .map(booking -> new UserBookingResponse(
                        booking.getId(),
                        booking.getSeat().getShow().getTitle(),
                        booking.getSeat().getShow().getVenue(),
                        booking.getSeat().getSeatNumber(),
                        booking.getSeat().getPrice(),
                        booking.getBookedAt()
                ))
                .collect(Collectors.toList());
    }

    public record UserBookingResponse(
            String bookingId,
            String showTitle,
            String venue,
            String seatNumber,
            int price,
            java.time.LocalDateTime bookedAt
    ) {}

   @Transactional
    public void cancelBooking(String bookingId, String userId) {
        // 1) 예매 조회
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("예매 내역을 찾을 수 없습니다."));
 
        // 2) 본인 예매인지 검증
        if (!booking.getUser().getUserId().equals(userId)) {
            throw new ConflictException(ErrorCode.ACCESS_DENIED);
        }
 
        Seat seat = booking.getSeat();
        Long showId = seat.getShow().getShowId();
        int price = seat.getPrice();
        Long seatId = seat.getId();
 
        // 3) 좌석 상태 복구 SOLD -> AVAILABLE (더티체킹)
        seat.available();
 
        // 4) 잔여석 +1 (showId + price로 등급 역매칭)
        seatGradeRepository.findFirstByShowIdAndPrice(showId, price)
                .ifPresent(grade -> seatGradeRepository.increaseRemainingSeats(grade.getId()));
 
        // 5) 예매 레코드 삭제
        bookingRepository.delete(booking);
 
        // 6) Redis sold 키 정리 (재예매 가능하도록)
        redisTemplate.delete("sold:" + seatId);
    }

    public record BookingAcceptResponse(String requestId, String status) {}
    public record BookingStatusResponse(String requestId, String status) {}
}
