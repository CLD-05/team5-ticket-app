package com.example.ticketing.booking.controller;

import com.example.ticketing.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    // Queue Token 검증 적용 시 X-Queue-Token 헤더를 추가로 받기
    // @RequestHeader(value = "X-Queue-Token", required = false) String queueToken
    @PostMapping
    public ResponseEntity<?> requestBooking(@RequestParam Long seatId, @AuthenticationPrincipal String userId) {
        // 적용 시: bookingService.requestBooking(seatId, userId, queueToken)
        var response = bookingService.requestBooking(seatId, userId);
        return ResponseEntity.accepted().body(response);
    }
    
    @GetMapping("/status/{requestId}")
    public ResponseEntity<?> getStatus(@PathVariable String requestId) {
    	return ResponseEntity.ok(bookingService.getBookingStatus(requestId));
    }
}
