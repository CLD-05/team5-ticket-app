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

    @PostMapping
    public ResponseEntity<?> requestBooking(@RequestParam Long seatId, @AuthenticationPrincipal String userId) {
        var response = bookingService.requestBooking(seatId, userId);
        return ResponseEntity.accepted().body(response);
    }
}
