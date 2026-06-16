package com.example.ticketing.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원가입 요청")
public class SignupRequest {
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "올바른 이메일 형식이어야 합니다.")
    @Schema(description = "로그인에 사용할 이메일", example = "user@example.com")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Size(min = 4, max = 20, message = "비밀번호는 4자 이상 20자 이하로 입력해주세요.")
    @Schema(description = "4자 이상 20자 이하 비밀번호", example = "password123")
    private String password;

    @NotBlank(message = "이름은 필수 입력값입니다.")
    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;
}
