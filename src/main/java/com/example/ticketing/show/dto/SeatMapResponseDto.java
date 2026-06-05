package com.example.ticketing.show.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SeatMapResponseDto {

    private Long showId;
    private String venueName;
    private List<SeatGradeDto> seatGrades;

    @Getter
    @Builder
    public static class SeatGradeDto {
        private String gradeName;
        private Integer price;
        private Integer totalSeats;
        private Integer remainingSeats;
    }
}