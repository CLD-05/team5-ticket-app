package com.example.ticketing.global.exception;

public class NotFoundException extends BusinessException {
    public NotFoundException(String message) {
        super(message, ErrorCode.NOT_FOUND);
    }
    public NotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
}
