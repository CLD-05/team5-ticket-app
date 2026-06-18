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

    public static ShowListResponseDto from(Show show) {
        return ShowListResponseDto.builder()
                .showId(show.getShowId())
                .title(show.getTitle())
                .venue(show.getVenue())
                .build();
    }
}
