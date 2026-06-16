package com.example.ticketing.user.controller;

import com.example.ticketing.global.exception.BusinessException;
import com.example.ticketing.global.exception.ErrorCode;
import com.example.ticketing.user.entity.User;
import com.example.ticketing.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "내 프로필 조회와 수정 API")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserRepository userRepository;

    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 ID, 이메일, 이름을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 프로필 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "userId": "user@example.com",
                                      "email": "user@example.com",
                                      "name": "홍길동"
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return ResponseEntity.ok(new UserResponse(user.getUserId(), user.getEmail(), user.getName()));
    }

    @Operation(summary = "내 프로필 수정", description = "현재 로그인한 사용자의 이름을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 프로필 수정 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "userId": "user@example.com",
                                      "email": "user@example.com",
                                      "name": "김철수"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "이름이 비어 있음", content = @Content),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
    })
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

    @Schema(description = "프로필 수정 요청")
    public record UpdateProfileRequest(
            @Schema(description = "변경할 사용자 이름", example = "김철수")
            String name
    ) {}

    @Schema(description = "사용자 프로필 응답")
    public record UserResponse(
            @Schema(description = "사용자 ID", example = "user@example.com")
            String userId,
            @Schema(description = "사용자 이메일", example = "user@example.com")
            String email,
            @Schema(description = "사용자 이름", example = "홍길동")
            String name
    ) {}
}
