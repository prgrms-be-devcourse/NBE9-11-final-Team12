#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
K6_SCRIPT="$ROOT_DIR/performance/k6/target-scale-mixed-limit.js"

calculate_estimated_http_rps() {
  local read_rate="$1"
  local write_rate="$2"
  local stage_rate="$3"

  # target-scale-mixed-limit.js 기준:
  # read iteration 1회 = HTTP GET 9개
  # write iteration 1회 = HTTP 약 2~3개
  # stage iteration 1회 = HTTP POST 1개
  echo $((read_rate * 9 + write_rate * 3 + stage_rate))
}

if [[ -z "${BASE_URL:-}" ]]; then
  cat <<'MSG' >&2
BASE_URL이 필요합니다.
예: BASE_URL=https://api.example.com performance/scripts/run-prod-load.sh smoke
MSG
  exit 1
fi

if ! command -v k6 >/dev/null 2>&1; then
  echo "k6를 찾을 수 없습니다. k6 설치 후 다시 실행하세요." >&2
  exit 1
fi

STAGE="${1:-smoke}"

case "$STAGE" in
  smoke)
    DURATION="${DURATION:-3m}"
    READ_RATE="${READ_RATE:-5}"
    WRITE_RATE="${WRITE_RATE:-1}"
    STAGE_RATE="${STAGE_RATE:-1}"
    WS_VUS="${WS_VUS:-50}"
    ;;
  baseline)
    DURATION="${DURATION:-5m}"
    READ_RATE="${READ_RATE:-15}"
    WRITE_RATE="${WRITE_RATE:-5}"
    STAGE_RATE="${STAGE_RATE:-5}"
    WS_VUS="${WS_VUS:-200}"
    ;;
  half)
    DURATION="${DURATION:-7m}"
    READ_RATE="${READ_RATE:-30}"
    WRITE_RATE="${WRITE_RATE:-10}"
    STAGE_RATE="${STAGE_RATE:-10}"
    WS_VUS="${WS_VUS:-500}"
    ;;
  target)
    DURATION="${DURATION:-10m}"
    READ_RATE="${READ_RATE:-55}"
    WRITE_RATE="${WRITE_RATE:-20}"
    STAGE_RATE="${STAGE_RATE:-20}"
    WS_VUS="${WS_VUS:-800}"
    ;;
  spike)
    DURATION="${DURATION:-2m}"
    READ_RATE="${READ_RATE:-75}"
    WRITE_RATE="${WRITE_RATE:-30}"
    STAGE_RATE="${STAGE_RATE:-30}"
    WS_VUS="${WS_VUS:-1000}"
    ;;
  *)
    echo "알 수 없는 단계입니다: $STAGE" >&2
    echo "사용 가능: smoke | baseline | half | target | spike" >&2
    exit 1
    ;;
esac

ROOM_IDS="${ROOM_IDS:-900001,900002,900003,900004,900005,900006,900007,900008,900009,900010}"
USER_ID_BASE="${USER_ID_BASE:-100000}"
USERS_PER_ROOM="${USERS_PER_ROOM:-100}"
SPEECH_ID_BASE="${SPEECH_ID_BASE:-910001}"
SPEECH_COUNT="${SPEECH_COUNT:-500}"
FRONTEND_ORIGIN="${FRONTEND_ORIGIN:-https://www.sisibibi.com}"
READ_PRE_ALLOCATED_VUS="${READ_PRE_ALLOCATED_VUS:-100}"
READ_MAX_VUS="${READ_MAX_VUS:-500}"
WRITE_PRE_ALLOCATED_VUS="${WRITE_PRE_ALLOCATED_VUS:-50}"
WRITE_MAX_VUS="${WRITE_MAX_VUS:-300}"
STAGE_PRE_ALLOCATED_VUS="${STAGE_PRE_ALLOCATED_VUS:-100}"
STAGE_MAX_VUS="${STAGE_MAX_VUS:-500}"
WS_MAX_DURATION="${WS_MAX_DURATION:-12m}"
CONNECTION_DURATION_SECONDS="${CONNECTION_DURATION_SECONDS:-60}"
MESSAGE_INTERVAL_SECONDS="${MESSAGE_INTERVAL_SECONDS:-5}"

ESTIMATED_HTTP_RPS="$(calculate_estimated_http_rps "$READ_RATE" "$WRITE_RATE" "$STAGE_RATE")"

cat <<MSG
[performance] 운영 부하 테스트 실행
- stage: $STAGE
- baseUrl: $BASE_URL
- duration: $DURATION
- read iterations/s: $READ_RATE (읽기 1회 = HTTP GET 9개)
- write iterations/s: $WRITE_RATE (쓰기 1회 = HTTP 약 2~3개)
- stage iterations/s: $STAGE_RATE
- estimated raw HTTP RPS: 약 $ESTIMATED_HTTP_RPS
- websocket VUs: $WS_VUS
- roomIds: $ROOM_IDS

중단 기준:
- HTTP 5xx >= 1%
- p95 3초 이상이 2분 이상 지속
- Hikari pending connection 지속 증가
- MySQL lock wait 또는 slow query 증가
- EC2 CPU 85% 이상이 3분 이상 지속
MSG

BASE_URL="$BASE_URL" \
FRONTEND_ORIGIN="$FRONTEND_ORIGIN" \
DURATION="$DURATION" \
READ_RATE="$READ_RATE" \
WRITE_RATE="$WRITE_RATE" \
STAGE_RATE="$STAGE_RATE" \
WS_VUS="$WS_VUS" \
ROOM_IDS="$ROOM_IDS" \
USER_ID_BASE="$USER_ID_BASE" \
USERS_PER_ROOM="$USERS_PER_ROOM" \
SPEECH_ID_BASE="$SPEECH_ID_BASE" \
SPEECH_COUNT="$SPEECH_COUNT" \
READ_PRE_ALLOCATED_VUS="$READ_PRE_ALLOCATED_VUS" \
READ_MAX_VUS="$READ_MAX_VUS" \
WRITE_PRE_ALLOCATED_VUS="$WRITE_PRE_ALLOCATED_VUS" \
WRITE_MAX_VUS="$WRITE_MAX_VUS" \
STAGE_PRE_ALLOCATED_VUS="$STAGE_PRE_ALLOCATED_VUS" \
STAGE_MAX_VUS="$STAGE_MAX_VUS" \
WS_MAX_DURATION="$WS_MAX_DURATION" \
CONNECTION_DURATION_SECONDS="$CONNECTION_DURATION_SECONDS" \
MESSAGE_INTERVAL_SECONDS="$MESSAGE_INTERVAL_SECONDS" \
k6 run "$K6_SCRIPT"
