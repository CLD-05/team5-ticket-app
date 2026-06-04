package com.example.ticketing.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "Invalid Input Value"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "Internal Server Error"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "Resource Not Found"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C004", "Access Denied"),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "Unauthorized access"),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A002", "Login failed"),

    // Business
    SEAT_ALREADY_HELD(HttpStatus.CONFLICT, "B001", "이미 선택된 좌석입니다."),
    SEAT_ALREADY_SOLD(HttpStatus.CONFLICT, "B002", "이미 판매된 좌석입니다."),
    HOLD_EXPIRED(HttpStatus.CONFLICT, "B003", "시간이 초과되어 좌석이 해제되었습니다."),
    QUEUE_NOT_FOUND(HttpStatus.NOT_FOUND, "B004", "대기열 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
