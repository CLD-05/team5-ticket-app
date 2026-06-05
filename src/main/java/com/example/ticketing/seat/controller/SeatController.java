package com.example.ticketing.seat.controller;

import com.example.ticketing.global.exception.ConflictException;
import com.example.ticketing.seat.dto.SeatResponseDto;
import com.example.ticketing.seat.service.SeatService;
import com.example.ticketing.seat.service.SeatService.SeatHoldResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/performances/{id}/seats")
    public ResponseEntity<List<SeatResponseDto>> getSeats(@PathVariable("id") Long id) {
        return ResponseEntity.ok(seatService.getSeats(id));
    }

    @PostMapping("/seats/{seatId}/hold")
    public ResponseEntity<SeatHoldResponse> holdSeat(@PathVariable("seatId") Long seatId, Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        return ResponseEntity.ok(seatService.holdSeat(seatId, userId));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<String> handleConflictException(ConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("ERR_SEAT_ALREADY_TAKEN");
    }
}
