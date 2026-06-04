package com.example.ticketing.global.exception;

public class ConflictException extends BusinessException {
    public ConflictException(String message) {
        super(message, ErrorCode.SEAT_ALREADY_HELD); // Default to held or specific
    }
    public ConflictException(ErrorCode errorCode) {
        super(errorCode);
    }
}
