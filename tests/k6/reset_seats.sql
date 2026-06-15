-- ============================================================
-- reset_seats.sql
-- 부하 테스트 후 / 데모 전 좌석·예매 데이터 초기화
-- ============================================================
-- 동작:
--   1) bookings 전체 삭제 (테스트 예매 기록 제거)
--   2) seats 전체를 AVAILABLE로 복구 (SOLD/HOLD 해제)
-- 잔여석은 별도 카운터가 아니라 seats에서 집계되므로 이 두 가지로 완전 복구됨.
--
-- ⚠️ dev 테스트 환경 전용. 보존할 실제 예매가 있으면 실행 금지.
-- ⚠️ 실행 시 모든 예매가 삭제되고 전 좌석이 열림 → 동시 작업 팀원에게 공지 후 실행.
-- ============================================================

START TRANSACTION;

-- [실행 전 현황]
SELECT '=== BEFORE ===' AS phase;
SELECT COUNT(*) AS bookings_before FROM bookings;
SELECT status, COUNT(*) AS cnt FROM seats GROUP BY status;

-- [1] 테스트 예매 전체 삭제
DELETE FROM bookings;

-- [2] 전 좌석 AVAILABLE 복구
UPDATE seats SET status = 'AVAILABLE' WHERE status <> 'AVAILABLE';

-- [실행 후 검증]
SELECT '=== AFTER ===' AS phase;
SELECT COUNT(*) AS bookings_after FROM bookings;          -- 0 이어야 함
SELECT status, COUNT(*) AS cnt FROM seats GROUP BY status; -- AVAILABLE 만 남아야 함

-- ============================================================
-- 위 검증 결과가 정상(bookings 0, seats 전부 AVAILABLE)이면 커밋:
--     COMMIT;
-- 이상하면 되돌리기:
--     ROLLBACK;
-- ============================================================