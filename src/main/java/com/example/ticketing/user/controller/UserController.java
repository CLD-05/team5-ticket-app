package com.example.ticketing.user.controller;

import com.example.ticketing.global.exception.BusinessException;
import com.example.ticketing.global.exception.ErrorCode;
import com.example.ticketing.user.entity.User;
import com.example.ticketing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return ResponseEntity.ok(new UserResponse(user.getUserId(), user.getEmail(), user.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @AuthenticationPrincipal String userId,
            @RequestBody UpdateProfileRequest request
    ) {
        if (request.name() == null || request.name().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        user.updateName(request.name());
        userRepository.save(user);
        
        return ResponseEntity.ok(new UserResponse(user.getUserId(), user.getEmail(), user.getName()));
    }

    public record UpdateProfileRequest(String name) {}
    public record UserResponse(String userId, String email, String name) {}
}
