package com.example.ticketing.seat.dto;

import com.example.ticketing.seat.entity.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SeatResponse {
    private Long id;
    private String seatNumber;
    private int price;
    private SeatStatus status;
}
