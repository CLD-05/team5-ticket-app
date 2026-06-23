package com.example.ticketing.show.dto;

import com.example.ticketing.show.entity.Show;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "공연 상세 응답")
public class ShowDetailResponseDto {

    @Schema(description = "공연 ID", example = "1")
    private Long showId;

    @Schema(description = "공연 제목", example = "S-Tier Concert")
    private String title;

    @Schema(description = "공연장", example = "KSPO DOME")
    private String venue;

    @Schema(description = "예매 오픈 시간 (Unix Timestamp)", example = "1782120000")
    private Long bookingOpenAt;

    @Schema(description = "예매 마감 시간 (Unix Timestamp)", example = "1782123600")
    private Long bookingCloseAt;

    @Schema(description = "공연 시작 시간 (Unix Timestamp)", example = "1782130000")
    private Long performanceAt;

    @Schema(description = "공연 이미지 URL")
    private String imageUrl;

    public static ShowDetailResponseDto from(Show show, Long bookingOpenAt, Long bookingCloseAt, Long performanceAt) {
        return ShowDetailResponseDto.builder()
                .showId(show.getShowId())
                .title(show.getTitle())
                .venue(show.getVenue())
                .bookingOpenAt(bookingOpenAt)
                .bookingCloseAt(bookingCloseAt)
                .performanceAt(performanceAt)
                .imageUrl(show.getImageUrl())
                .build();
    }
}
