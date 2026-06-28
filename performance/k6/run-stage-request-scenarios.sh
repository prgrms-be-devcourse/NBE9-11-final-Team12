#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-sisibibi-mysql}"
MYSQL_DATABASE="${MYSQL_DATABASE:-sisibibi}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"
REDIS_CONTAINER="${REDIS_CONTAINER:-sisibibi-redis}"
ROOM_BASE="${ROOM_BASE:-970001}"
USER_BASE="${USER_BASE:-970000000}"
MULTI_ROOM_COUNT="${MULTI_ROOM_COUNT:-15}"
SUMMARY_DIR="${SUMMARY_DIR:-/tmp/sisibibi-stage-request-results}"
MAX_DURATION="${MAX_DURATION:-120s}"
RUN_SCENARIOS="${RUN_SCENARIOS:-all}"

mkdir -p "$SUMMARY_DIR"

mysql_exec() {
  docker exec -e MYSQL_PWD="$MYSQL_PASSWORD" -i "$MYSQL_CONTAINER" mysql \
    -u"$MYSQL_USER" \
    "$MYSQL_DATABASE" \
    --batch \
    --raw
}

mysql_scalar() {
  docker exec -e MYSQL_PWD="$MYSQL_PASSWORD" -i "$MYSQL_CONTAINER" mysql \
    -u"$MYSQL_USER" \
    "$MYSQL_DATABASE" \
    --batch \
    --raw \
    --skip-column-names
}

has_room_queue_sequences_table() {
  local table_count
  table_count="$(mysql_scalar <<SQL
SELECT COUNT(*)
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'room_queue_sequences';
SQL
)"

  [[ "$table_count" == "1" ]]
}

rooms_csv() {
  local room_count="$1"
  local rooms=""

  for ((i = 0; i < room_count; i++)); do
    local room_id=$((ROOM_BASE + i))
    if [[ -n "$rooms" ]]; then
      rooms+=","
    fi
    rooms+="$room_id"
  done

  printf '%s' "$rooms"
}

cleanup_redis() {
  local room_count="$1"

  for ((i = 0; i < room_count; i++)); do
    local room_id=$((ROOM_BASE + i))
    docker exec "$REDIS_CONTAINER" redis-cli DEL \
      "stage:queue:{$room_id}" \
      "stage:current:{$room_id}" \
      "stage:projection-version:{$room_id}" >/dev/null
  done
}

cleanup_mysql() {
  local total_users="$1"
  local room_count="$2"
  local user_end=$((USER_BASE + total_users - 1))
  local room_end=$((ROOM_BASE + room_count - 1))

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
SET FOREIGN_KEY_CHECKS = 1;
SQL

  if has_room_queue_sequences_table; then
    mysql_exec <<SQL >/dev/null
DELETE FROM room_queue_sequences
WHERE room_id BETWEEN ${ROOM_BASE} AND ${room_end};
SQL
  fi
}

setup_mysql() {
  local users_per_room="$1"
  local room_count="$2"
  local total_users=$((users_per_room * room_count))

  mysql_exec <<SQL >/dev/null
SET FOREIGN_KEY_CHECKS = 0;

INSERT INTO topics (id, title, description, category, source_url, status, created_at, approved_at, approved_by)
SELECT
  ${ROOM_BASE} + seq.n,
  CONCAT('perf-stage-topic-', ${ROOM_BASE} + seq.n),
  'stage request performance test topic',
  'PERF',
  'https://example.com/perf-stage',
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
WHERE seq.n < ${room_count};

INSERT INTO rooms (id, topic_id, title, status, started_at, ended_at, max_participants, created_at)
SELECT
  ${ROOM_BASE} + seq.n,
  ${ROOM_BASE} + seq.n,
  CONCAT('perf-stage-room-', ${ROOM_BASE} + seq.n),
  'OPEN',
  NOW(6),
  DATE_ADD(NOW(6), INTERVAL 1 DAY),
  ${users_per_room} + 100,
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
WHERE seq.n < ${room_count};

INSERT INTO users (id, email, password, nickname, role, status, token_version, created_at, updated_at)
SELECT
  ${USER_BASE} + seq.n,
  CONCAT('perf-stage-', ${USER_BASE} + seq.n, '@sisibibi.test'),
  'noop-password',
  CONCAT('perf_stage_', ${USER_BASE} + seq.n),
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
  ${ROOM_BASE} + MOD(seq.n, ${room_count}),
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

SET FOREIGN_KEY_CHECKS = 1;
SQL

  if has_room_queue_sequences_table; then
    mysql_exec <<SQL >/dev/null
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
WHERE seq.n < ${room_count};
SQL
  fi
}

inspect_mysql() {
  local room_count="$1"
  local room_end=$((ROOM_BASE + room_count - 1))

  mysql_exec <<SQL
SELECT
  room_id,
  COUNT(*) AS total_rows,
  COUNT(DISTINCT queue_order) AS distinct_orders,
  MIN(queue_order) AS min_order,
  MAX(queue_order) AS max_order,
  SUM(status = 'WAITING') AS waiting_rows,
  SUM(status = 'ASSIGNED') AS assigned_rows
FROM speaking_queue
WHERE room_id BETWEEN ${ROOM_BASE} AND ${room_end}
GROUP BY room_id
ORDER BY room_id;
SQL
}

inspect_redis() {
  local room_count="$1"

  printf 'room_id\tredis_waiting\n'
  for ((i = 0; i < room_count; i++)); do
    local room_id=$((ROOM_BASE + i))
    local count
    count="$(docker exec "$REDIS_CONTAINER" redis-cli ZCARD "stage:queue:{$room_id}")"
    printf '%s\t%s\n' "$room_id" "$count"
  done
}

parse_summary() {
  local scenario="$1"
  local expected="$2"
  local summary_file="$3"
  local db_file="$4"
  local redis_file="$5"
  local exit_code="$6"

  node - "$scenario" "$expected" "$summary_file" "$db_file" "$redis_file" "$exit_code" <<'NODE'
const fs = require("fs");
const [scenario, expected, summaryFile, dbFile, redisFile, exitCode] = process.argv.slice(2);
const summary = JSON.parse(fs.readFileSync(summaryFile, "utf8"));
const duration = summary.metrics.http_req_duration ?? {};
const failed = summary.metrics.http_req_failed?.rate ?? 0;
const created = summary.metrics.stage_requests_created?.count ?? "";
const rejected = summary.metrics.stage_requests_rejected?.count ?? "";
const checksRate = summary.metrics.checks?.value ?? 0;
const httpReqsRate = summary.metrics.http_reqs?.rate ?? 0;
const dbRows = fs.existsSync(dbFile)
  ? fs.readFileSync(dbFile, "utf8").trim().split(/\n/).slice(1).filter(Boolean)
  : [];
const redisRows = fs.existsSync(redisFile)
  ? fs.readFileSync(redisFile, "utf8").trim().split(/\n/).slice(1).filter(Boolean)
  : [];
const dbTotal = dbRows.reduce((sum, row) => sum + Number(row.split(/\t/)[1] || 0), 0);
const redisTotal = redisRows.reduce((sum, row) => sum + Number(row.split(/\t/)[1] || 0), 0);

console.log([
  scenario,
  expected,
  created,
  rejected,
  dbTotal,
  redisTotal,
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
  local room_count="$2"
  local users_per_room="$3"
  local total_users=$((room_count * users_per_room))
  local room_ids
  local summary_file="${SUMMARY_DIR}/${scenario}.json"
  local db_file="${SUMMARY_DIR}/${scenario}-db.tsv"
  local redis_file="${SUMMARY_DIR}/${scenario}-redis.tsv"
  local k6_log="${SUMMARY_DIR}/${scenario}-k6.log"
  local exit_code=0

  room_ids="$(rooms_csv "$room_count")"

  printf '\n[%s] 1. 테스트 환경 세팅\n' "$scenario" >&2
  cleanup_mysql "$total_users" "$room_count"
  cleanup_redis "$room_count"
  setup_mysql "$users_per_room" "$room_count"

  printf '[%s] 2. 테스트 스크립트 진행: rooms=%s, usersPerRoom=%s, total=%s\n' \
    "$scenario" "$room_count" "$users_per_room" "$total_users" >&2

  BASE_URL="$BASE_URL" \
  ROOM_IDS="$room_ids" \
  USER_ID_BASE="$USER_BASE" \
  USERS_PER_ROOM="$users_per_room" \
  VUS="$total_users" \
  ITERATIONS="$total_users" \
  MAX_DURATION="$MAX_DURATION" \
  TOKEN_VERSION=0 \
  K6_SUMMARY_TREND_STATS="avg,min,med,max,p(90),p(95),p(99)" \
  k6 run \
    --summary-export "$summary_file" \
    performance/k6/stage-request-rooms.js >"$k6_log" 2>&1 || exit_code=$?

  inspect_mysql "$room_count" >"$db_file"
  inspect_redis "$room_count" >"$redis_file"

  printf '[%s] 3. 테스트 환경 초기화\n' "$scenario" >&2
  cleanup_mysql "$total_users" "$room_count"
  cleanup_redis "$room_count"

  parse_summary "$scenario" "$total_users" "$summary_file" "$db_file" "$redis_file" "$exit_code"
}

should_run_scenario() {
  local scenario="$1"

  [[ "$RUN_SCENARIOS" == "all" || ",${RUN_SCENARIOS}," == *",${scenario},"* ]]
}

append_scenario() {
  local scenario="$1"
  local room_count="$2"
  local users_per_room="$3"

  if should_run_scenario "$scenario"; then
    run_scenario "$scenario" "$room_count" "$users_per_room" >>"${SUMMARY_DIR}/summary.tsv"
  fi
}

printf 'scenario\texpected\tcreated\trejected\tdb_rows\tredis_waiting\tavg_ms\tp95_ms\tp99_ms\treq_per_sec\thttp_failed\tchecks\texit_code\n' \
  >"${SUMMARY_DIR}/summary.tsv"

append_scenario "single-room-50" 1 50
append_scenario "single-room-100" 1 100
append_scenario "multi-15rooms-50" "$MULTI_ROOM_COUNT" 50
append_scenario "multi-15rooms-100" "$MULTI_ROOM_COUNT" 100

printf '\n결과 요약: %s\n' "${SUMMARY_DIR}/summary.tsv"
cat "${SUMMARY_DIR}/summary.tsv"
