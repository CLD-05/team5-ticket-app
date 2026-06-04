package com.example.ticketing.booking.controller;

import com.example.ticketing.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<?> requestBooking(@RequestParam Long seatId, @AuthenticationPrincipal User principal) {
        var response = bookingService.requestBooking(seatId, principal.getUsername());
        return ResponseEntity.accepted().body(response);
    }
}
