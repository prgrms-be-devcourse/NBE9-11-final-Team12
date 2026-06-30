#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-sisibibi-mysql}"
MYSQL_DATABASE="${MYSQL_DATABASE:-sisibibi}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"
REDIS_CONTAINER="${REDIS_CONTAINER:-sisibibi-redis}"
ROOM_BASE="${ROOM_BASE:-980001}"
USER_BASE="${USER_BASE:-980000000}"
ROOM_COUNT="${ROOM_COUNT:-15}"
USERS_PER_ROOM="${USERS_PER_ROOM:-400}"
SUMMARY_DIR="${SUMMARY_DIR:-/tmp/sisibibi-stage-request-rate-results}"
RUN_SCENARIOS="${RUN_SCENARIOS:-all}"

SCENARIO_NAMES=("rate-50" "rate-100" "rate-200" "rate-300")
SCENARIO_RATES=(50 100 200 300)
SCENARIO_DURATIONS=("20s" "20s" "20s" "20s")
SCENARIO_PRE_ALLOCATED_VUS=(50 80 120 180)
SCENARIO_MAX_VUS=(150 250 400 600)

mkdir -p "$SUMMARY_DIR"

duration_seconds() {
  local duration="$1"
  if [[ "$duration" =~ ^([0-9]+)s$ ]]; then
    printf '%s' "${BASH_REMATCH[1]}"
    return
  fi

  printf '지원하지 않는 duration 형식: %s\n' "$duration" >&2
  exit 1
}

validate_user_capacity() {
  local total_users=$((ROOM_COUNT * USERS_PER_ROOM))
  local max_required_users=0

  for i in "${!SCENARIO_NAMES[@]}"; do
    local scenario="${SCENARIO_NAMES[$i]}"
    local rate="${SCENARIO_RATES[$i]}"
    local duration="${SCENARIO_DURATIONS[$i]}"

    if ! should_run_scenario "$scenario"; then
      continue
    fi

    local required_users=$((rate * $(duration_seconds "$duration")))
    if (( required_users > max_required_users )); then
      max_required_users=$required_users
    fi
  done

  if (( total_users < max_required_users )); then
    printf '테스트 사용자 수가 부족합니다. totalUsers=%s, requiredUsers=%s\n' \
      "$total_users" "$max_required_users" >&2
    printf 'ROOM_COUNT 또는 USERS_PER_ROOM을 늘린 뒤 다시 실행하세요.\n' >&2
    exit 1
  fi
}

mysql_exec() {
  docker exec -e MYSQL_PWD="$MYSQL_PASSWORD" -i "$MYSQL_CONTAINER" mysql \
    -u"$MYSQL_USER" \
    "$MYSQL_DATABASE" \
    --batch \
    --raw
}

rooms_csv() {
  local rooms=""
  for ((i = 0; i < ROOM_COUNT; i++)); do
    local room_id=$((ROOM_BASE + i))
    if [[ -n "$rooms" ]]; then
      rooms+=","
    fi
    rooms+="$room_id"
  done
  printf '%s' "$rooms"
}

cleanup_redis() {
  for ((i = 0; i < ROOM_COUNT; i++)); do
    local room_id=$((ROOM_BASE + i))
    docker exec "$REDIS_CONTAINER" redis-cli DEL \
      "stage:queue:{$room_id}" \
      "stage:current:{$room_id}" \
      "stage:projection-version:{$room_id}" >/dev/null
  done
}

cleanup_mysql() {
  local total_users=$((ROOM_COUNT * USERS_PER_ROOM))
  local user_end=$((USER_BASE + total_users - 1))
  local room_end=$((ROOM_BASE + ROOM_COUNT - 1))

  mysql_exec <<SQL >/dev/null
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM speaking_queue
WHERE (room_id BETWEEN ${ROOM_BASE} AND ${room_end})
   OR (user_id BETWEEN ${USER_BASE} AND ${user_end});
DELETE FROM room_participants
WHERE (room_id BETWEEN ${ROOM_BASE} AND ${room_end})
   OR (user_id BETWEEN ${USER_BASE} AND ${user_end});
DELETE FROM speeches
WHERE (room_id BETWEEN ${ROOM_BASE} AND ${room_end})
   OR (user_id BETWEEN ${USER_BASE} AND ${user_end});
DELETE FROM chat_messages
WHERE (room_id BETWEEN ${ROOM_BASE} AND ${room_end})
   OR (user_id BETWEEN ${USER_BASE} AND ${user_end});
DELETE FROM rooms
WHERE id BETWEEN ${ROOM_BASE} AND ${room_end}
   OR topic_id BETWEEN ${ROOM_BASE} AND ${room_end};
DELETE FROM topics
WHERE id BETWEEN ${ROOM_BASE} AND ${room_end};
DELETE FROM users
WHERE id BETWEEN ${USER_BASE} AND ${user_end};
DELETE FROM room_queue_sequences
WHERE room_id BETWEEN ${ROOM_BASE} AND ${room_end};
SET FOREIGN_KEY_CHECKS = 1;
SQL
}

setup_mysql() {
  local total_users=$((ROOM_COUNT * USERS_PER_ROOM))

  mysql_exec <<SQL >/dev/null
SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO topics (id, title, description, category, source_url, status, created_at, approved_at, approved_by)
SELECT
  ${ROOM_BASE} + seq.n,
  CONCAT('perf-stage-rate-topic-', ${ROOM_BASE} + seq.n),
  'stage request rate performance test topic',
  'PERF',
  'https://example.com/perf-stage-rate',
  'APPROVED',
  NOW(6),
  NOW(6),
  NULL
FROM (
  SELECT ones.n + tens.n * 10 AS n
  FROM
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
  CROSS JOIN
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
) seq
WHERE seq.n < ${ROOM_COUNT};

INSERT INTO rooms (id, topic_id, title, status, started_at, ended_at, max_participants, created_at)
SELECT
  ${ROOM_BASE} + seq.n,
  ${ROOM_BASE} + seq.n,
  CONCAT('perf-stage-rate-room-', ${ROOM_BASE} + seq.n),
  'OPEN',
  NOW(6),
  DATE_ADD(NOW(6), INTERVAL 1 DAY),
  ${USERS_PER_ROOM} + 100,
  NOW(6)
FROM (
  SELECT ones.n + tens.n * 10 AS n
  FROM
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
  CROSS JOIN
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
) seq
WHERE seq.n < ${ROOM_COUNT};

INSERT INTO users (id, email, password, nickname, role, status, token_version, created_at, updated_at)
SELECT
  ${USER_BASE} + seq.n,
  CONCAT('perf-stage-rate-', ${USER_BASE} + seq.n, '@sisibibi.test'),
  'noop-password',
  CONCAT('perf_stage_rate_', ${USER_BASE} + seq.n),
  'USER',
  'ACTIVE',
  0,
  NOW(6),
  NOW(6)
FROM (
  SELECT ones.n + tens.n * 10 + hundreds.n * 100 + thousands.n * 1000 AS n
  FROM
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
  CROSS JOIN
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
  CROSS JOIN
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) hundreds
  CROSS JOIN
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) thousands
) seq
WHERE seq.n < ${total_users};

INSERT INTO room_participants (room_id, user_id, joined_at, left_at, status)
SELECT
  ${ROOM_BASE} + MOD(seq.n, ${ROOM_COUNT}),
  ${USER_BASE} + seq.n,
  NOW(6),
  NULL,
  'JOINED'
FROM (
  SELECT ones.n + tens.n * 10 + hundreds.n * 100 + thousands.n * 1000 AS n
  FROM
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
  CROSS JOIN
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
  CROSS JOIN
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) hundreds
  CROSS JOIN
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) thousands
) seq
WHERE seq.n < ${total_users};

INSERT INTO room_queue_sequences (room_id, next_queue_order, created_at, updated_at)
SELECT
  ${ROOM_BASE} + seq.n,
  1,
  NOW(6),
  NOW(6)
FROM (
  SELECT ones.n + tens.n * 10 AS n
  FROM
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
  CROSS JOIN
    (SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
     UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
) seq
WHERE seq.n < ${ROOM_COUNT};

SET FOREIGN_KEY_CHECKS = 1;
SQL
}

parse_summary() {
  local scenario="$1"
  local summary_file="$2"
  local exit_code="$3"

  node - "$scenario" "$summary_file" "$exit_code" <<'NODE'
const fs = require("fs");
const [scenario, summaryFile, exitCode] = process.argv.slice(2);
const summary = JSON.parse(fs.readFileSync(summaryFile, "utf8"));
const duration = summary.metrics.http_req_duration ?? {};
const failed = summary.metrics.http_req_failed?.rate ?? 0;
const created = summary.metrics.stage_requests_created?.count ?? "";
const rejected = summary.metrics.stage_requests_rejected?.count ?? "";
const checksRate = summary.metrics.checks?.value ?? 0;
const httpReqsRate = summary.metrics.http_reqs?.rate ?? 0;

console.log([
  scenario,
  created,
  rejected,
  Number(duration.avg ?? 0).toFixed(2),
  Number(duration["p(95)"] ?? 0).toFixed(2),
  Number(duration["p(99)"] ?? 0).toFixed(2),
  Number(httpReqsRate).toFixed(2),
  (failed * 100).toFixed(2) + "%",
  (checksRate * 100).toFixed(2) + "%",
  exitCode
].join("\t"));
NODE
}

run_scenario() {
  local scenario="$1"
  local rate="$2"
  local duration="$3"
  local pre_allocated_vus="$4"
  local max_vus="$5"
  local summary_file="${SUMMARY_DIR}/${scenario}.json"
  local k6_log="${SUMMARY_DIR}/${scenario}-k6.log"
  local exit_code=0
  local room_ids

  room_ids="$(rooms_csv)"

  printf '\n[%s] 1. 테스트 환경 세팅\n' "$scenario" >&2
  cleanup_mysql
  cleanup_redis
  setup_mysql

  printf '[%s] 2. RATE=%s, DURATION=%s, rooms=%s, usersPerRoom=%s\n' \
    "$scenario" "$rate" "$duration" "$ROOM_COUNT" "$USERS_PER_ROOM" >&2

  BASE_URL="$BASE_URL" \
  ROOM_IDS="$room_ids" \
  USER_ID_BASE="$USER_BASE" \
  USERS_PER_ROOM="$USERS_PER_ROOM" \
  RATE="$rate" \
  DURATION="$duration" \
  PRE_ALLOCATED_VUS="$pre_allocated_vus" \
  MAX_VUS="$max_vus" \
  TOKEN_VERSION=0 \
  K6_SUMMARY_TREND_STATS="avg,min,med,max,p(90),p(95),p(99)" \
  k6 run \
    --summary-export "$summary_file" \
    performance/k6/stage-request-rooms-rate.js >"$k6_log" 2>&1 || exit_code=$?

  printf '[%s] 3. 테스트 환경 초기화\n' "$scenario" >&2
  cleanup_mysql
  cleanup_redis

  parse_summary "$scenario" "$summary_file" "$exit_code"
}

should_run_scenario() {
  local scenario="$1"
  [[ "$RUN_SCENARIOS" == "all" || ",${RUN_SCENARIOS}," == *",${scenario},"* ]]
}

append_scenario() {
  local scenario="$1"
  local rate="$2"
  local duration="$3"
  local pre_allocated_vus="$4"
  local max_vus="$5"

  if should_run_scenario "$scenario"; then
    run_scenario "$scenario" "$rate" "$duration" "$pre_allocated_vus" "$max_vus" >>"${SUMMARY_DIR}/summary.tsv"
  fi
}

printf 'scenario\tcreated\trejected\tavg_ms\tp95_ms\tp99_ms\treq_per_sec\thttp_failed\tchecks\texit_code\n' \
  >"${SUMMARY_DIR}/summary.tsv"

validate_user_capacity

for i in "${!SCENARIO_NAMES[@]}"; do
  append_scenario \
    "${SCENARIO_NAMES[$i]}" \
    "${SCENARIO_RATES[$i]}" \
    "${SCENARIO_DURATIONS[$i]}" \
    "${SCENARIO_PRE_ALLOCATED_VUS[$i]}" \
    "${SCENARIO_MAX_VUS[$i]}"
done

printf '\n결과 요약: %s\n' "${SUMMARY_DIR}/summary.tsv"
cat "${SUMMARY_DIR}/summary.tsv"
