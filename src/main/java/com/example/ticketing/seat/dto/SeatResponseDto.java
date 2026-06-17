package com.example.ticketing.seat.dto;

import com.example.ticketing.seat.entity.SeatStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "좌석 응답")
public class SeatResponseDto {
    @Schema(description = "좌석 ID", example = "101")
    private final Long seatId;

    @Schema(description = "공연 ID", example = "1")
    private final Long showId;

    @Schema(description = "좌석 번호", example = "A-1")
    private final String seatNumber;

    @Schema(description = "좌석 가격", example = "150000")
    private final Integer price;

    @Schema(description = "좌석 상태", example = "AVAILABLE")
    private final SeatStatus status;

    @Schema(description = "내가 선점한 좌석 여부", example = "false")
    private final Boolean isMyHold;
}
