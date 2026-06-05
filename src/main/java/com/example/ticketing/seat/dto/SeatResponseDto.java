package com.example.ticketing.seat.dto;

import com.example.ticketing.seat.entity.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SeatResponseDto {
    private final Long seatId;
    private final Long showId;
    private final String seatNumber;
    private final Integer price;
    private final SeatStatus status;
}
