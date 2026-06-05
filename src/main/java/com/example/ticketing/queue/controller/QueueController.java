package com.example.ticketing.queue.controller;

import com.example.ticketing.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shows/{showId}/queue")
@RequiredArgsConstructor
public class QueueController {
    private final QueueService queueService;

    // 공연별 대기열에 사용자 진입
    @PostMapping("/join")
    public ResponseEntity<?> join(@PathVariable Long showId, @AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(queueService.joinQueue(showId, principal.getUsername()));
    }

    // 현재 사용자의 공연별 대기 순번 조회
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@PathVariable Long showId, @AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(queueService.getStatus(showId, principal.getUsername()));
    }

    // 공연별 대기열에서 사용자 이탈
    @DeleteMapping("/leave")
    public ResponseEntity<?> leave(@PathVariable Long showId, @AuthenticationPrincipal User principal) {
        queueService.leaveQueue(showId, principal.getUsername());
        return ResponseEntity.ok().build();
    }
}
