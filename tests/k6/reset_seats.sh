#!/usr/bin/env bash
# ============================================================
# reset_seats.sh
# 부하 테스트 후 / 데모 전 좌석·예매 리셋을 한 번에 실행.
# bastion(또는 kubectl 가능한 곳)에서 실행.
#
# 사용법:
#   1) DB 비밀번호를 환경변수로 지정 (화면 노출 방지):
#        export DB_PASSWORD='실제비밀번호'
#   2) 실행:
#        bash reset_seats.sh
#
# ⚠️ dev 전용. 모든 예매 삭제 + 전 좌석 개방. 팀원 공지 후 실행.
# ============================================================
set -euo pipefail

DB_HOST="team5-dev-rds-proxy.proxy-cx482c6a63zp.ap-northeast-2.rds.amazonaws.com"
DB_USER="ticketadmin"
DB_NAME="ticketing"
NS="ticketing"

if [[ -z "${DB_PASSWORD:-}" ]]; then
  echo "ERROR: DB_PASSWORD 환경변수를 먼저 설정하세요."
  echo "  export DB_PASSWORD='...'"
  exit 1
fi

echo "[reset_seats] 좌석/예매 리셋 시작..."

# kubectl PATH 보정 (SSM 세션에서 자주 누락됨)
export PATH="$HOME/bin:$PATH"

kubectl run mysql-reset --rm -i --image=mysql:8 -n "$NS" --restart=Never \
  --env="MYSQL_PWD=${DB_PASSWORD}" -- \
  mysql -h "$DB_HOST" -u "$DB_USER" "$DB_NAME" <<'SQL'
START TRANSACTION;

SELECT '=== BEFORE ===' AS phase;
SELECT COUNT(*) AS bookings_before FROM bookings;
SELECT status, COUNT(*) AS cnt FROM seats GROUP BY status;

DELETE FROM bookings;
UPDATE seats SET status = 'AVAILABLE' WHERE status <> 'AVAILABLE';

SELECT '=== AFTER ===' AS phase;
SELECT COUNT(*) AS bookings_after FROM bookings;
SELECT status, COUNT(*) AS cnt FROM seats GROUP BY status;

COMMIT;
SELECT '=== DONE (committed) ===' AS phase;
SQL

echo "[reset_seats] 완료. 좌석 페이지 새로고침(Ctrl+Shift+R)으로 확인하세요."
echo "[reset_seats] Redis HOLD 잔재가 있으면 아래로 추가 정리:"
echo "  kubectl run redis-cli --rm --image=redis:7 -n $NS --restart=Never --attach -- \\"
echo "    redis-cli -h team5-dev-redis.50cal1.ng.0001.apn2.cache.amazonaws.com -p 6379 --scan --pattern 'seat:*'"