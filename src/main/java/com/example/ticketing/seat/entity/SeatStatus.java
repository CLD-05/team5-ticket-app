package com.example.ticketing.seat.entity;

public enum SeatStatus {
    AVAILABLE("예매 가능"),
    HOLD("임시 선점"),
    SOLD("예매 완료");

    private final String description;

    SeatStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
