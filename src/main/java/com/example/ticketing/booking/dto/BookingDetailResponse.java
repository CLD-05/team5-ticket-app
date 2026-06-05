package com.example.ticketing.booking.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record BookingDetailResponse(
    String bookingId,
    Long seatId,
    String seatNumber,
    int price,
    Long showId,
    String showTitle,
    String userId,
    LocalDateTime bookedAt
) implements Serializable {}
