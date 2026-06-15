package com.example.ticketing.seat.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

import com.example.ticketing.global.exception.ConflictException;
import com.example.ticketing.seat.dto.SeatResponseDto;
import com.example.ticketing.seat.service.SeatService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/performances/{id}/seats")
    public ResponseEntity<List<SeatResponseDto>> getSeats(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(seatService.getSeats(id));
    }

    @PostMapping("/seats/{seatId}/hold")
    public ResponseEntity<?> holdSeat(
            @PathVariable Long seatId,
            @AuthenticationPrincipal String userId
    ) {
        var response = seatService.holdSeat(seatId, userId);
        return ResponseEntity.ok(response);
    }
    
     @DeleteMapping("/seats/{seatId}/hold")
    public ResponseEntity<?> releaseSeat(
            @PathVariable Long seatId,
            @AuthenticationPrincipal String userId
    ) {
        seatService.releaseSeat(seatId, userId);
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<String> handleConflictException(ConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("ERR_SEAT_ALREADY_TAKEN");
    }
}
