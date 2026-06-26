package com.example.ticketing.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponse(
        @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "사용자 ID", example = "user@example.com")
        String userId,
        @Schema(description = "사용자 이름", example = "홍길동")
        String name
) {

}
