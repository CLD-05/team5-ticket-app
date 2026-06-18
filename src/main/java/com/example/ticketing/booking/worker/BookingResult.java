package com.example.ticketing.booking.worker;

/**
 * 예매 확정 처리 결과. status 응답 / Redis 저장값 / 메트릭 라벨을 하나의 vocabulary로 통일한다.
 *   - CONFIRMED : 확정 성공 (이미 SOLD인 메시지 재처리=멱등 포함)
 *   - CONFLICT  : 동시성 패배 (DB Unique 위반). 정상적 거절이라 SLO 성공률 분모에서 제외 대상.
 *   - FAILED    : 시스템 실패 (좌석/사용자 없음 등 진짜 오류).
 * 미처리(PROCESSING)는 Worker가 아직 결과를 안 쓴 상태라 여기 없음(BookingService에서 표현).
 */
public enum BookingResult {
    CONFIRMED,
    CONFLICT,
    FAILED;

    /** Prometheus 메트릭 라벨용 소문자 표기 → booking_confirm_total{result="..."} */
    public String metricLabel() {
        return name().toLowerCase();
    }
}