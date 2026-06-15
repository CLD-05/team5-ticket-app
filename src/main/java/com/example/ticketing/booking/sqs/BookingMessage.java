package com.example.ticketing.booking.sqs;

import java.io.Serializable;

public record BookingMessage(
    String requestId,
    Long showId,
    Long seatId,
    String userId,
    String queueToken,
    long createdAtEpochMs // 큐 인입 시각 (e2e 확정시간 측정용)
) implements Serializable {}
