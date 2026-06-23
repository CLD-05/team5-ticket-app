package com.example.ticketing.show.dto;

import com.example.ticketing.show.entity.Show;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "공연 목록 응답")
public class ShowListResponseDto {

    @Schema(description = "공연 ID", example = "1")
    private Long showId;

    @Schema(description = "공연 제목", example = "S-Tier Concert")
    private String title;

    @Schema(description = "공연장", example = "KSPO DOME")
    private String venue;

    @Schema(description = "공연 이미지 URL", example = "http://localhost:4566/team5-dev-poster-bucket/image.jpg")
    private String imageUrl;

    public static ShowListResponseDto from(Show show) {
        return ShowListResponseDto.builder()
                .showId(show.getShowId())
                .title(show.getTitle())
                .venue(show.getVenue())
                .imageUrl(show.getImageUrl())
                .build();
    }
}
