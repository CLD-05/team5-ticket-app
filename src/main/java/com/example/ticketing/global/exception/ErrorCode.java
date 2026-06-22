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
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증에 실패했습니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A002", "로그인에 실패했습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "A003", "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "A004", "유효하지 않은 토큰입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A005", "사용자를 찾을 수 없습니다."),

    // Business
    SEAT_ALREADY_HELD(HttpStatus.CONFLICT, "B001", "이미 선택된 좌석입니다."),
    SEAT_ALREADY_SOLD(HttpStatus.CONFLICT, "B002", "이미 판매된 좌석입니다."),
    HOLD_EXPIRED(HttpStatus.CONFLICT, "B003", "시간이 초과되어 좌석이 해제되었습니다."),
    QUEUE_NOT_FOUND(HttpStatus.NOT_FOUND, "B004", "대기열 정보를 찾을 수 없습니다."),
    QUEUE_TOKEN_REQUIRED(HttpStatus.FORBIDDEN, "B005", "Queue Token이 필요합니다."),
    INVALID_QUEUE_TOKEN(HttpStatus.FORBIDDEN, "B006", "유효하지 않은 Queue Token입니다."),
    QUEUE_TOKEN_EXPIRED(HttpStatus.FORBIDDEN, "B007", "Queue Token이 만료되었습니다."),
    BOOKING_NOT_OPEN(HttpStatus.FORBIDDEN, "B008", "예매 오픈 전입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
