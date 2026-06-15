package com.example.ticketing.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@NoArgsConstructor
@Schema(description = "로그인 요청")
public class LoginRequest {
    @NotBlank
    @Email
    @Schema(description = "회원 이메일", example = "user@example.com")
    private String email;

    @NotBlank
    @Schema(description = "비밀번호", example = "password123")
    private String password;
}
