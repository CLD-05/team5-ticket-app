package com.example.ticketing.booking.sqs;

import java.io.Serializable;

public record BookingMessage(
    String requestId,
    Long seatId,
    String userId
) implements Serializable {}
