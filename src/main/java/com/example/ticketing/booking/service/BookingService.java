package com.example.ticketing.booking.service;

import com.example.ticketing.booking.dto.BookingDetailResponse;
import com.example.ticketing.booking.dto.BookingStatusResponse;
import com.example.ticketing.booking.entity.Booking;
import com.example.ticketing.booking.repository.BookingRepository;
import com.example.ticketing.booking.sqs.BookingMessage;
import com.example.ticketing.global.exception.BusinessException;
import com.example.ticketing.global.exception.ConflictException;
import com.example.ticketing.global.exception.ErrorCode;
import com.example.ticketing.global.exception.NotFoundException;
import com.example.ticketing.seat.entity.Seat;
import com.example.ticketing.seat.entity.SeatStatus;
import com.example.ticketing.seat.repository.SeatRepository;
import com.example.ticketing.user.entity.User;
import com.example.ticketing.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
    private final StringRedisTemplate redisTemplate;
    private final SqsTemplate sqsTemplate;
    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public BookingAcceptResponse requestBooking(Long seatId, String userId) {
        String holdKey = "seat:" + seatId;
        String requestId = UUID.randomUUID().toString();

        // Redis Lua Script: HOLD 여부 확인 및 즉시 삭제(Settle) 후 SOLD 처리
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

        // Set request status to PROCESSING in Redis
        redisTemplate.opsForValue().set("booking:status:" + requestId, "PROCESSING", Duration.ofMinutes(30));

        // SQS Message 발행 (비동기 처리 요청)
        sqsTemplate.send("booking-queue", new BookingMessage(requestId, seatId, userId));

        return new BookingAcceptResponse(requestId, "ACCEPTED");
    }

    @Transactional
    public Booking confirmBooking(BookingMessage message) {
        // 1. Acquire pessimistic lock
        Seat seat = seatRepository.findByIdWithLock(message.seatId())
                .orElseThrow(() -> new NotFoundException("좌석을 찾을 수 없습니다."));

        if (seat.getStatus() == SeatStatus.SOLD) {
            throw new ConflictException(ErrorCode.SEAT_ALREADY_SOLD);
        }

        User user = userRepository.findById(message.userId())
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        // 2. Change status to SOLD
        seat.sold();
        seatRepository.save(seat);

        // 3. Save booking (Unique constraint is the final safety net)
        Booking booking = new Booking(seat, user);
        Booking savedBooking = bookingRepository.save(booking);

        // 4. Cache detailed booking data to prevent CQRS replication lag issues
        BookingDetailResponse detail = new BookingDetailResponse(
                savedBooking.getId(),
                seat.getId(),
                seat.getSeatNumber(),
                seat.getPrice(),
                seat.getShow().getId(),
                seat.getShow().getTitle(),
                user.getUserId(),
                savedBooking.getBookedAt()
        );

        try {
            redisTemplate.opsForValue().set("booking:cache:" + savedBooking.getId(),
                    objectMapper.writeValueAsString(detail), Duration.ofMinutes(5));
        } catch (Exception e) {
            log.error("Failed to serialize and cache booking details for: {}", savedBooking.getId(), e);
        }

        return savedBooking;
    }

    public BookingStatusResponse getBookingStatus(String requestId) {
        String key = "booking:status:" + requestId;
        String val = redisTemplate.opsForValue().get(key);
        if (val == null) {
            throw new NotFoundException("예매 요청을 찾을 수 없습니다.");
        }

        if (val.startsWith("SUCCESS:")) {
            String bookingId = val.substring(8);
            return new BookingStatusResponse("SUCCESS", bookingId);
        } else if ("FAILED".equals(val)) {
            return new BookingStatusResponse("FAILED", null);
        } else {
            return new BookingStatusResponse("PROCESSING", null);
        }
    }

    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingDetails(String bookingId) {
        String cacheKey = "booking:cache:" + bookingId;
        String cachedJson = redisTemplate.opsForValue().get(cacheKey);
        if (cachedJson != null) {
            try {
                return objectMapper.readValue(cachedJson, BookingDetailResponse.class);
            } catch (Exception e) {
                log.warn("Failed to read cached booking details for: {}", bookingId, e);
            }
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("예매 내역을 찾을 수 없습니다."));

        BookingDetailResponse response = new BookingDetailResponse(
                booking.getId(),
                booking.getSeat().getId(),
                booking.getSeat().getSeatNumber(),
                booking.getSeat().getPrice(),
                booking.getSeat().getShow().getId(),
                booking.getSeat().getShow().getTitle(),
                booking.getUser().getUserId(),
                booking.getBookedAt()
        );

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), Duration.ofMinutes(5));
        } catch (Exception e) {
            log.error("Failed to cache booking details for: {}", bookingId, e);
        }

        return response;
    }

    @Transactional
    public void cancelBooking(String bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("예매 내역을 찾을 수 없습니다."));

        if (!booking.getUser().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        Seat seat = booking.getSeat();

        // 1. Delete booking
        bookingRepository.delete(booking);

        // 2. Set seat status back to AVAILABLE
        seat.available();
        seatRepository.save(seat);

        // 3. Clear Redis keys
        redisTemplate.delete("sold:" + seat.getId());
        redisTemplate.delete("booking:cache:" + bookingId);
    }

    public record BookingAcceptResponse(String requestId, String status) {}
}
