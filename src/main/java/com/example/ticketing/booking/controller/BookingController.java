package com.example.ticketing.booking.controller;

import com.example.ticketing.booking.service.BookingService;
import com.example.ticketing.global.exception.BusinessException;
import com.example.ticketing.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<?> requestBooking(
            @RequestParam(required = false) Long seatId,
            @RequestBody(required = false) BookingRequest request,
            @AuthenticationPrincipal String userId,
            @RequestHeader(value = "X-Queue-Token", required = false) String queueTokenHeader,
            @CookieValue(value = "queueToken", required = false) String queueTokenCookie
    ) {
        Long resolvedSeatId = resolveSeatId(seatId, request);
        String queueToken = resolveQueueToken(queueTokenHeader, queueTokenCookie);

        var response = bookingService.requestBooking(resolvedSeatId, userId, queueToken);
        return ResponseEntity.accepted().body(response);
    }
    
    @GetMapping("/status/{requestId}")
    public ResponseEntity<?> getStatus(@PathVariable String requestId) {
    	return ResponseEntity.ok(bookingService.getBookingStatus(requestId));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyBookings(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(bookingService.getUserBookings(userId));
    }

    private Long resolveSeatId(Long seatId, BookingRequest request) {
        Long resolvedSeatId = seatId != null ? seatId : request != null ? request.seatId() : null;
        if (resolvedSeatId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return resolvedSeatId;
    }

    private String resolveQueueToken(String queueTokenHeader, String queueTokenCookie) {
        return StringUtils.hasText(queueTokenHeader) ? queueTokenHeader : queueTokenCookie;
    }

    public record BookingRequest(Long seatId) {}
}
