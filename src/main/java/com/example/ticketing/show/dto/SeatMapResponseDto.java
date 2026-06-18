package com.example.ticketing.show.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "공연별 좌석 등급 현황 응답")
public class SeatMapResponseDto {

    @Schema(description = "공연 ID", example = "1")
    private Long showId;

    @Schema(description = "공연장 이름", example = "KSPO DOME")
    private String venueName;

    @Schema(description = "좌석 등급별 잔여 현황")
    private List<SeatGradeDto> seatGrades;

    @Getter
    @Builder
    @Schema(description = "좌석 등급 정보")
    public static class SeatGradeDto {
        @Schema(description = "좌석 등급명", example = "VIP")
        private String gradeName;

        @Schema(description = "좌석 가격", example = "150000")
        private Integer price;

        @Schema(description = "전체 좌석 수", example = "100")
        private Integer totalSeats;

        @Schema(description = "잔여 좌석 수", example = "42")
        private Integer remainingSeats;
    }
}
