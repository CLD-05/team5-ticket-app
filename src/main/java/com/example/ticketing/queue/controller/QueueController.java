package com.example.ticketing.queue.controller;

import com.example.ticketing.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController {
    private final QueueService queueService;

    @PostMapping("/join")
    public ResponseEntity<?> join(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(queueService.joinQueue(userId));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(queueService.getStatus(userId));
    }

    @DeleteMapping("/leave")
    public ResponseEntity<?> leave(@AuthenticationPrincipal String userId) {
        queueService.leaveQueue(userId);
        return ResponseEntity.ok().build();
    }
}
