package com.example.ticketing.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "신규 공연 등록 요청")
public class CreateShowRequest {
    @Schema(description = "공연 제목", example = "임영웅 콘서트 IM HERO")
    private String title;

    @Schema(description = "공연장", example = "서울월드컵경기장")
    private String venue;

    @Schema(description = "예매 오픈 시각 (Unix Timestamp)", example = "1782120000")
    private Long bookingOpenAt;

    @Schema(description = "예매 마감 시각 (Unix Timestamp)", example = "1782123600")
    private Long bookingCloseAt;

    @Schema(description = "공연 실제 시작 시각 (Unix Timestamp)", example = "1782130000")
    private Long performanceAt;
}
