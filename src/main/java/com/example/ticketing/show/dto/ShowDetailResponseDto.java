package com.example.ticketing.show.dto;

import com.example.ticketing.show.entity.Show;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ShowDetailResponseDto {

    private Long showId;
    private String title;
    private String venue;

    public static ShowDetailResponseDto from(Show show) {
        return ShowDetailResponseDto.builder()
                .showId(show.getShowId())
                .title(show.getTitle())
                .venue(show.getVenue())
                .build();
    }
}