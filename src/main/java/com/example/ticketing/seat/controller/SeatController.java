package com.example.ticketing.seat.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import com.example.ticketing.global.exception.ConflictException;
import com.example.ticketing.seat.service.SeatService;
 
import lombok.RequiredArgsConstructor;
 
@RestController
@RequestMapping("/api/v1/seats") 
@RequiredArgsConstructor
public class SeatController {
 
    private final SeatService seatService;
 
    @PostMapping("/{seatId}/hold")
    public ResponseEntity<?> holdSeat(@PathVariable Long seatId, @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(seatService.holdSeat(seatId, userId));
    }
 
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<String> handleConflictException(ConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("ERR_SEAT_ALREADY_TAKEN");
    }
}