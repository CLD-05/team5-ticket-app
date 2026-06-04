package com.example.ticketing.seat.controller;

import com.example.ticketing.seat.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
public class SeatController {
    private final SeatService seatService;

    @PostMapping("/{seatId}/hold")
    public ResponseEntity<?> holdSeat(@PathVariable Long seatId, @AuthenticationPrincipal User principal) {
        var response = seatService.holdSeat(seatId, principal.getUsername());
        return ResponseEntity.ok(response);
    }
}
