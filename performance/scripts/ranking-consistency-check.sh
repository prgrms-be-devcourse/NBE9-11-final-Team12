#!/bin/bash
# 랭킹 정합성 검사: ZSCORE == log(1+p)*0.5 + log(1+c)*0.3 + log(1+r)*0.2 인지
# 부하가 도는 동안 원자적으로(EVAL 1회) 반복 샘플링해서 위반 비율을 센다.
#
# 사용법:
#   ./ranking-consistency-check.sh lua 70
#   ./ranking-consistency-check.sh redis 70
#
# k6 시작 직전에 실행할 것. (k6 60s + 정착 10s = 70)

set -u

MODE_LABEL=${1:-run}
SECS=${2:-70}

REDIS_CONTAINER=${REDIS_CONTAINER:-redis_1}
REDIS_PW=${REDIS_PW:-lldj123414}
ROOM_ID=${ROOM_ID:-900001}
INTERVAL=${INTERVAL:-0.005}          # 5ms = 초당 200샘플
TOLERANCE=${TOLERANCE:-1e-9}         # Java Math.log vs Lua math.log 마지막 비트 차이 흡수

OUT="/tmp/ranking-samples-${MODE_LABEL}.csv"
N=$(awk -v s="$SECS" -v i="$INTERVAL" 'BEGIN{printf "%d", s/i}')

RCLI=(sudo docker exec "$REDIS_CONTAINER" redis-cli -a "$REDIS_PW" --no-auth-warning)

LUA='local room = ARGV[1]
local p = tonumber(redis.call("HGET", KEYS[1], room) or "0")
local c = tonumber(redis.call("HGET", KEYS[2], room) or "0")
local r = tonumber(redis.call("HGET", KEYS[3], room) or "0")
local s = redis.call("ZSCORE", KEYS[4], room)
if not s then return "nil,0,0,0,0,0" end
local expected = math.log(1+p)*0.5 + math.log(1+c)*0.3 + math.log(1+r)*0.2
return string.format("%d,%d,%d,%.17g,%.17g,%.17g",
                     p, c, r, tonumber(s), expected, math.abs(tonumber(s)-expected))'

echo "[1/3] 스크립트 로드..."
SHA=$("${RCLI[@]}" SCRIPT LOAD "$LUA") || { echo "SCRIPT LOAD 실패"; exit 1; }
echo "      sha=$SHA"

echo "[2/3] 샘플링 시작 (mode=$MODE_LABEL, room=$ROOM_ID, ${SECS}s, ${N}샘플)"
echo "      >>> 지금 다른 터미널에서 k6 실행하세요 <<<"
"${RCLI[@]}" -r "$N" -i "$INTERVAL" EVALSHA "$SHA" 4 \
  room:ranking:participant-count \
  room:ranking:chat-message-count \
  room:ranking:reaction-count \
  room:ranking \
  "$ROOM_ID" > "$OUT"

echo "[3/3] 집계"
echo "----------------------------------------------------"
awk -F, -v tol="$TOLERANCE" -v mode="$MODE_LABEL" '
  $1 != "nil" && NF == 6 {
    n++
    if ($6 + 0 > tol + 0) { v++ }
    last_p=$1; last_c=$2; last_r=$3; last_s=$4; last_e=$5; last_d=$6
  }
  END {
    if (n == 0) { print "샘플 없음 - 컨테이너명/비밀번호/room id 확인"; exit 1 }
    printf "mode          : %s\n", mode
    printf "samples       : %d\n", n
    printf "violations    : %d\n", v+0
    printf "violation rate: %.4f %%\n", (v+0)*100/n
    print  "----------------------------------------------------"
    printf "최종 상태  p=%s c=%s r=%s\n", last_p, last_c, last_r
    printf "           ZSCORE   = %s\n", last_s
    printf "           expected = %s\n", last_e
    printf "           diff     = %s  -> %s\n", last_d, (last_d+0 > tol+0 ? "불일치 X" : "일치 O")
  }' "$OUT"
echo "----------------------------------------------------"
echo "원본: $OUT"
