package com.example.ticketing.seat.controller;

import com.example.ticketing.seat.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
public class SeatController {
    private final SeatService seatService;

    // Queue Token 검증 적용 시 X-Queue-Token 헤더를 추가로 받기
    // @RequestHeader(value = "X-Queue-Token", required = false) String queueToken
    @PostMapping("/{seatId}/hold")
    public ResponseEntity<?> holdSeat(@PathVariable Long seatId, @AuthenticationPrincipal String userId) {
        // 적용 시: seatService.holdSeat(seatId, userId, queueToken)
        var response = seatService.holdSeat(seatId, userId);
        return ResponseEntity.ok(response);
    }
}
