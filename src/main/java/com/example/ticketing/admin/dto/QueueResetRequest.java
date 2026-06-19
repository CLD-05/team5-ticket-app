package com.example.ticketing.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대기열 강제 리셋 요청")
public class QueueResetRequest {
    @Schema(description = "공연 ID (생략 시 전체 공연 대기열 리셋)", example = "1")
    private Long showId;
}
