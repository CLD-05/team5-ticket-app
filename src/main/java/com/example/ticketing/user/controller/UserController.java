package com.example.ticketing.user.controller;

import com.example.ticketing.user.entity.User;
import com.example.ticketing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow();
        return ResponseEntity.ok(new UserResponse(user.getUserId(), user.getEmail(), user.getName()));
    }

    public record UserResponse(String userId, String email, String name) {}
}
