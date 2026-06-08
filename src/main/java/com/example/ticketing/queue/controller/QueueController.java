package com.example.ticketing.queue.controller;

import com.example.ticketing.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shows/{showId}/queue")
@RequiredArgsConstructor
public class QueueController {
    private final QueueService queueService;

    // 공연별 대기열에 사용자 진입
    @PostMapping("/join")
    public ResponseEntity<?> join(@PathVariable Long showId, @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(queueService.joinQueue(showId, userId));
    }

    // 예매 진입 시 대기열 화면이 필요한지 먼저 판단
    @PostMapping("/enter")
    public ResponseEntity<?> enter(@PathVariable Long showId, @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(queueService.enterQueue(showId, userId));
    }

    // 현재 사용자의 공연별 대기 순번 조회
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@PathVariable Long showId, @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(queueService.getStatus(showId, userId));
    }

    // 공연별 대기열에서 사용자 이탈
    @DeleteMapping("/leave")
    public ResponseEntity<?> leave(@PathVariable Long showId, @AuthenticationPrincipal String userId) {
        queueService.leaveQueue(showId, userId);
        return ResponseEntity.ok().build();
    }
}
