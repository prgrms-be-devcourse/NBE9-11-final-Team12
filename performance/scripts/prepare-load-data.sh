#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SEED_SQL="$ROOT_DIR/performance/sql/seed-performance-data.sql"
CLEANUP_SQL="$ROOT_DIR/performance/sql/cleanup-performance-data.sql"

if [[ "${CONFIRM_PERFORMANCE_DATA_WRITE:-}" != "YES" ]]; then
  cat <<'MSG' >&2
성능 테스트 데이터 준비는 DB에 쓰기 작업을 수행합니다.
실행하려면 아래 환경 변수를 명시하세요.

  CONFIRM_PERFORMANCE_DATA_WRITE=YES

운영 서버에서 실행할 경우 반드시 팀 합의된 테스트 시간대와 대상 DB를 확인하세요.
MSG
  exit 1
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-sisibibi}"
DB_USERNAME="${DB_USERNAME:-root}"
DB_PASSWORD="${DB_PASSWORD:-root}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"
export MYSQL_PWD="$DB_PASSWORD"

if ! command -v "$MYSQL_BIN" >/dev/null 2>&1; then
  cat <<MSG >&2
mysql client를 찾을 수 없습니다.
- EC2에 mysql client를 설치하거나
- MYSQL_BIN에 실행 가능한 mysql client 경로를 지정하세요.
MSG
  exit 1
fi

cat <<MSG
[performance] 테스트 데이터 준비 시작
- host: $DB_HOST:$DB_PORT
- db: $DB_NAME
- user: $DB_USERNAME
- cleanup: $CLEANUP_SQL
- seed: $SEED_SQL
MSG

"$MYSQL_BIN" \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --user="$DB_USERNAME" \
  --database="$DB_NAME" \
  < "$CLEANUP_SQL" \
  || { echo "[performance] cleanup failed" >&2; exit 1; }

"$MYSQL_BIN" \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --user="$DB_USERNAME" \
  --database="$DB_NAME" \
  < "$SEED_SQL" \
  || { echo "[performance] seed failed" >&2; exit 1; }

cat <<'MSG'
[performance] 테스트 데이터 준비 완료
기본 k6 데이터 범위:
- ROOM_IDS=900001,900002,900003,900004,900005,900006,900007,900008,900009,900010
- USER_ID_BASE=100000
- USERS_PER_ROOM=100
- SPEECH_ID_BASE=910001
- SPEECH_COUNT=500
MSG
