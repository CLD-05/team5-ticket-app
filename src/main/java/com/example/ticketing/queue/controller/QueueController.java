package com.example.ticketing.queue.controller;

import com.example.ticketing.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/queue")
@RequiredArgsConstructor
public class QueueController {
    private final QueueService queueService;

    @PostMapping("/join")
    public ResponseEntity<?> join(@AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(queueService.joinQueue(principal.getUsername()));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(queueService.getStatus(principal.getUsername()));
    }

    @DeleteMapping("/leave")
    public ResponseEntity<?> leave(@AuthenticationPrincipal User principal) {
        queueService.leaveQueue(principal.getUsername());
        return ResponseEntity.ok().build();
    }
}
