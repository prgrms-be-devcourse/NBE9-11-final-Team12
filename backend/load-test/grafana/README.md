# k6 Grafana Visualization

## Purpose

This local stack visualizes k6 load-test metrics with Grafana.

Flow:

```text
k6 -> Prometheus remote write -> Grafana
k6 -> InfluxDB -> Grafana
```

## Start Grafana, Prometheus, And InfluxDB

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/grafana
docker compose up -d
```

Open Grafana:

```text
http://localhost:3001
```

Default login:

```text
id: admin
password: admin
```

Prometheus is automatically registered as the default Grafana datasource.
InfluxDB is also registered as `InfluxDB-k6`.

Provisioned dashboard:

```text
Dashboards -> Load Test -> k6 InfluxDB Overview
```

Check InfluxDB:

```bash
curl http://localhost:8086/ping
```

InfluxDB returns `204 No Content` when it is healthy.

## Run k6 With Prometheus Remote Write

Run this from the k6 script directory.

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

K6_PROMETHEUS_RW_SERVER_URL=http://localhost:9090/api/v1/write \
K6_PROMETHEUS_RW_TREND_STATS=p(90),p(95),p(99),avg,min,max \
k6 run -o experimental-prometheus-rw ./stage-speaking-queue-load.js \
  -e BASE_URL=http://localhost:8080 \
  -e ROOM_ID=1 \
  -e ROOM_COUNT=10 \
  -e USER_ID_START=1 \
  -e USERS_PER_ROOM=10 \
  -e USER_COUNT=100 \
  -e SERVICE_CAPACITY_VUS=100 \
  -e SERVICE_CAPACITY_ITERATIONS=100 \
  -e SCENARIO=service_capacity_request_spike \
  --tag testid=service-capacity-100
```

## Run k6 With InfluxDB

Run this from the k6 script directory.

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

k6 run -o influxdb=http://localhost:8086/k6 ./stage-speaking-queue-load.js \
  -e BASE_URL=http://localhost:8080 \
  -e ROOM_ID=1 \
  -e ROOM_COUNT=10 \
  -e USER_ID_START=1 \
  -e USERS_PER_ROOM=10 \
  -e USER_COUNT=100 \
  -e SERVICE_CAPACITY_VUS=100 \
  -e SERVICE_CAPACITY_ITERATIONS=100 \
  -e SCENARIO=service_capacity_request_spike \
  --tag testid=service-capacity-100-influxdb
```

### Docker k6 With InfluxDB

Use this when k6 also runs inside Docker.

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

MSYS_NO_PATHCONV=1 docker run --rm \
  -v "$(pwd -W):/scripts" \
  --network grafana_default \
  grafana/k6 run -o influxdb=http://influxdb:8086/k6 /scripts/stage-speaking-queue-load.js \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e ROOM_ID=1 \
  -e ROOM_COUNT=10 \
  -e USER_ID_START=1 \
  -e USERS_PER_ROOM=10 \
  -e USER_COUNT=100 \
  -e SERVICE_CAPACITY_VUS=100 \
  -e SERVICE_CAPACITY_ITERATIONS=100 \
  -e SCENARIO=service_capacity_request_spike \
  --tag testid=service-capacity-100-influxdb-docker
```

### Docker k6 With InfluxDB Through Nginx

Use this after starting `sisibibi-stage-lb`.

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

MSYS_NO_PATHCONV=1 docker run --rm \
  -v "$(pwd -W):/scripts" \
  --network grafana_default \
  grafana/k6 run -o influxdb=http://influxdb:8086/k6 /scripts/stage-speaking-queue-load.js \
  -e BASE_URL=http://sisibibi-stage-lb:8088 \
  -e ROOM_ID=1 \
  -e ROOM_COUNT=10 \
  -e USER_ID_START=1 \
  -e USERS_PER_ROOM=50 \
  -e USER_COUNT=500 \
  -e SERVICE_CAPACITY_VUS=500 \
  -e SERVICE_CAPACITY_ITERATIONS=500 \
  -e HTTP_REQ_DURATION_P95_THRESHOLD_MS=5000 \
  -e HTTP_REQ_DURATION_P99_THRESHOLD_MS=8000 \
  -e SCENARIO=service_capacity_request_spike \
  --tag testid=service-capacity-500-nginx-hikari10-influxdb
```

For 1000 users, change these values:

```bash
-e USERS_PER_ROOM=100 \
-e USER_COUNT=1000 \
-e SERVICE_CAPACITY_VUS=1000 \
-e SERVICE_CAPACITY_ITERATIONS=1000 \
-e HTTP_REQ_DURATION_P95_THRESHOLD_MS=10000 \
-e HTTP_REQ_DURATION_P99_THRESHOLD_MS=12000 \
--tag testid=service-capacity-1000-nginx-hikari10-influxdb
```

## Run k6 In Docker

When k6 runs in Docker, `localhost` means the k6 container.
Use `host.docker.internal` to call the Spring Boot app running on the host machine.

The Docker-based Grafana run uses a separate observation threshold:

```text
local k6 threshold: p95 < 1000ms
docker k6 + Grafana threshold: p95 < 1500ms
```

This keeps local CLI results and Docker/Grafana observation results comparable within their own execution environment.

### 10 Rooms x 10 Users = 100 Requests

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

MSYS_NO_PATHCONV=1 docker run --rm \
  -v "$(pwd -W):/scripts" \
  --network grafana_default \
  -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
  -e K6_PROMETHEUS_RW_TREND_STATS=p\(90\),p\(95\),p\(99\),avg,min,max \
  grafana/k6 run -o experimental-prometheus-rw /scripts/stage-speaking-queue-load.js \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e ROOM_ID=1 \
  -e ROOM_COUNT=10 \
  -e USER_ID_START=1 \
  -e USERS_PER_ROOM=10 \
  -e USER_COUNT=100 \
  -e SERVICE_CAPACITY_VUS=100 \
  -e SERVICE_CAPACITY_ITERATIONS=100 \
  -e HTTP_REQ_DURATION_P95_THRESHOLD_MS=1500 \
  -e HTTP_REQ_DURATION_P99_THRESHOLD_MS=2000 \
  -e SCENARIO=service_capacity_request_spike \
  --tag testid=service-capacity-100-docker
```

### 10 Rooms x 20 Users = 200 Requests

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

MSYS_NO_PATHCONV=1 docker run --rm \
  -v "$(pwd -W):/scripts" \
  --network grafana_default \
  -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
  -e K6_PROMETHEUS_RW_TREND_STATS=p\(90\),p\(95\),p\(99\),avg,min,max \
  grafana/k6 run -o experimental-prometheus-rw /scripts/stage-speaking-queue-load.js \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e ROOM_ID=1 \
  -e ROOM_COUNT=10 \
  -e USER_ID_START=1 \
  -e USERS_PER_ROOM=20 \
  -e USER_COUNT=200 \
  -e SERVICE_CAPACITY_VUS=200 \
  -e SERVICE_CAPACITY_ITERATIONS=200 \
  -e HTTP_REQ_DURATION_P95_THRESHOLD_MS=1500 \
  -e HTTP_REQ_DURATION_P99_THRESHOLD_MS=2000 \
  -e SCENARIO=service_capacity_request_spike \
  --tag testid=service-capacity-200-docker
```

### Batched Prefill 500 Then Operate

This scenario avoids inserting 500 users in a single spike.
It inserts 250 users twice, then runs read and transition checks on the 500-user queue state.

Flow:

```text
0s: 250 request prefill
10s: next 250 request prefill
25s: waiting queue lookup, 10 iterations by default
50s: my position lookup, 500 iterations by default
75s: complete current speaker and auto grant next
```

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

MSYS_NO_PATHCONV=1 docker run --rm \
  -v "$(pwd -W):/scripts" \
  --network grafana_default \
  -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
  -e K6_PROMETHEUS_RW_TREND_STATS=p\(90\),p\(95\),p\(99\),avg,min,max \
  grafana/k6 run -o experimental-prometheus-rw /scripts/stage-speaking-queue-load.js \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e ROOM_ID=1 \
  -e ROOM_COUNT=10 \
  -e USER_ID_START=1 \
  -e BATCHED_PREFILL_BATCH_SIZE=250 \
  -e BATCHED_PREFILL_BATCH_COUNT=2 \
  -e BATCHED_PREFILL_USERS_PER_ROOM=25 \
  -e BATCHED_PREFILL_VUS=250 \
  -e BATCHED_QUEUE_LOOKUP_VUS=10 \
  -e BATCHED_QUEUE_LOOKUP_ITERATIONS=10 \
  -e BATCHED_POSITION_LOOKUP_VUS=10 \
  -e BATCHED_POSITION_LOOKUP_ITERATIONS=500 \
  -e BATCHED_CYCLE_VUS=10 \
  -e BATCHED_CYCLE_ITERATIONS=10 \
  -e HTTP_REQ_DURATION_P95_THRESHOLD_MS=5000 \
  -e HTTP_REQ_DURATION_P99_THRESHOLD_MS=8000 \
  -e SCENARIO=batched_prefill_500_then_operate \
  --tag testid=batched-prefill-500-operate-docker
```

### Hot Room Batched Prefill 500 Then Operate

This scenario puts all 500 users into one room.
Use it to observe a single hot room where every request touches the same `room_id`.

Flow:

```text
0s: room 1, 250 request prefill
10s: room 1, next 250 request prefill
25s: room 1 waiting queue lookup, 10 iterations
50s: room 1 my position lookup, 500 iterations
75s: room 1 complete current speaker and auto grant next, 1 iteration
```

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

MSYS_NO_PATHCONV=1 docker run --rm \
  -v "$(pwd -W):/scripts" \
  --network grafana_default \
  -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
  -e K6_PROMETHEUS_RW_TREND_STATS=p\(90\),p\(95\),p\(99\),avg,min,max \
  grafana/k6 run -o experimental-prometheus-rw /scripts/stage-speaking-queue-load.js \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e ROOM_ID=1 \
  -e ROOM_COUNT=1 \
  -e USER_ID_START=1 \
  -e BATCHED_PREFILL_BATCH_SIZE=250 \
  -e BATCHED_PREFILL_BATCH_COUNT=2 \
  -e BATCHED_PREFILL_USERS_PER_ROOM=250 \
  -e BATCHED_PREFILL_VUS=250 \
  -e BATCHED_QUEUE_LOOKUP_VUS=10 \
  -e BATCHED_QUEUE_LOOKUP_ITERATIONS=10 \
  -e BATCHED_POSITION_LOOKUP_VUS=10 \
  -e BATCHED_POSITION_LOOKUP_ITERATIONS=500 \
  -e BATCHED_CYCLE_VUS=1 \
  -e BATCHED_CYCLE_ITERATIONS=1 \
  -e HTTP_REQ_DURATION_P95_THRESHOLD_MS=8000 \
  -e HTTP_REQ_DURATION_P99_THRESHOLD_MS=12000 \
  -e SCENARIO=hot_room_batched_prefill_500_then_operate \
  --tag testid=hot-room-batched-500-operate-docker
```

### Hot Room Setup Prefill 500 Then Operate

This scenario minimizes idle waiting.
It sends 250 requests in `setup()` using `http.batch`, immediately sends the next 250 after the first batch completes, then starts the operation scenarios.

Use this when you want:

```text
250 request batch -> next 250 request batch -> queue/position/cycle checks
```

without fixed waiting gaps between the prefill batches.

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

MSYS_NO_PATHCONV=1 docker run --rm \
  -v "$(pwd -W):/scripts" \
  --network grafana_default \
  -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
  -e K6_PROMETHEUS_RW_TREND_STATS=p\(90\),p\(95\),p\(99\),avg,min,max \
  grafana/k6 run -o experimental-prometheus-rw /scripts/stage-speaking-queue-load.js \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e ROOM_ID=1 \
  -e ROOM_COUNT=1 \
  -e USER_ID_START=1 \
  -e BATCHED_PREFILL_BATCH_SIZE=250 \
  -e BATCHED_PREFILL_BATCH_COUNT=2 \
  -e BATCHED_PREFILL_USERS_PER_ROOM=250 \
  -e BATCHED_QUEUE_LOOKUP_VUS=10 \
  -e BATCHED_QUEUE_LOOKUP_ITERATIONS=10 \
  -e BATCHED_POSITION_LOOKUP_VUS=10 \
  -e BATCHED_POSITION_LOOKUP_ITERATIONS=500 \
  -e BATCHED_CYCLE_VUS=1 \
  -e BATCHED_CYCLE_ITERATIONS=1 \
  -e HTTP_REQ_DURATION_P95_THRESHOLD_MS=8000 \
  -e HTTP_REQ_DURATION_P99_THRESHOLD_MS=12000 \
  -e SCENARIO=hot_room_setup_prefill_500_then_operate \
  --tag testid=hot-room-setup-prefill-500-operate-docker
```

### Hot Room Position Polling 500

This scenario creates a 500-user queue in room 1 during `setup()`, then repeatedly calls `GET /requests/me/position`.
Use it to measure DB read pressure from clients polling their own waiting position.

Default:

```text
setup: room 1, 250 + 250 request prefill
load: 50 VUs for 30s
target API: GET /api/v1/rooms/1/stage/requests/me/position
```

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

MSYS_NO_PATHCONV=1 docker run --rm \
  -v "$(pwd -W):/scripts" \
  --network grafana_default \
  -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
  -e K6_PROMETHEUS_RW_TREND_STATS=p\(90\),p\(95\),p\(99\),avg,min,max \
  grafana/k6 run -o experimental-prometheus-rw /scripts/stage-speaking-queue-load.js \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e ROOM_ID=1 \
  -e ROOM_COUNT=1 \
  -e USER_ID_START=1 \
  -e BATCHED_PREFILL_BATCH_SIZE=250 \
  -e BATCHED_PREFILL_BATCH_COUNT=2 \
  -e BATCHED_PREFILL_USERS_PER_ROOM=250 \
  -e HOT_ROOM_POSITION_POLLING_VUS=50 \
  -e HOT_ROOM_POSITION_POLLING_DURATION=30s \
  -e HTTP_REQ_DURATION_P95_THRESHOLD_MS=1000 \
  -e HTTP_REQ_DURATION_P99_THRESHOLD_MS=2000 \
  -e SCENARIO=hot_room_position_polling_500 \
  --tag testid=hot-room-position-polling-500-50vu-30s
```

### Request Arrival Rate

This scenario sends a fixed number of request-speaking-turn calls over a controlled time window.
Use it when a raw VU spike fails before the requests reach the RDB queue path.

Default example:

```text
500 requests over 10s
rate: 50 requests/s
model: 10 rooms x 50 users
```

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

MSYS_NO_PATHCONV=1 docker run --rm \
  -p 5665:5665 \
  -v "$(pwd -W):/scripts" \
  --network grafana_default \
  -e K6_WEB_DASHBOARD=true \
  -e K6_WEB_DASHBOARD_HOST=0.0.0.0 \
  -e K6_WEB_DASHBOARD_PORT=5665 \
  -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
  -e K6_PROMETHEUS_RW_TREND_STATS=p\(90\),p\(95\),p\(99\),avg,min,max \
  grafana/k6 run -o experimental-prometheus-rw /scripts/stage-speaking-queue-load.js \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e ROOM_ID=1 \
  -e ROOM_COUNT=10 \
  -e USERS_PER_ROOM=50 \
  -e USER_ID_START=1 \
  -e USER_COUNT=500 \
  -e REQUEST_ARRIVAL_RATE=50 \
  -e REQUEST_ARRIVAL_DURATION=10s \
  -e REQUEST_ARRIVAL_PRE_ALLOCATED_VUS=100 \
  -e REQUEST_ARRIVAL_MAX_VUS=500 \
  -e HTTP_REQ_DURATION_P95_THRESHOLD_MS=5000 \
  -e HTTP_REQ_DURATION_P99_THRESHOLD_MS=8000 \
  -e SCENARIO=request_arrival_rate \
  --tag testid=request-arrival-500-over-10s
```

For a single hot room, change:

```bash
-e ROOM_COUNT=1 \
-e USERS_PER_ROOM=500 \
--tag testid=hot-room-request-arrival-500-over-10s
```

### Continuous Stage Operation

This scenario keeps the service under sustained mixed load.

It sends speaking-turn requests at a fixed arrival rate while another scenario completes the current speaker every 20 seconds.
Completion triggers the existing automatic next-speaker assignment path.

Default starting point:

```text
rooms: 10
request rate: 25 requests/s
request distribution: round-robin across rooms
rotation: 1 current speaker completion per room every 20s
duration: 3m
minimum seed users: 5000
```

Use this to observe a more service-like RDB Queue workload:

```text
continuous writes: request speaking turn
periodic state transition: ASSIGNED -> COMPLETED
automatic grant: next WAITING -> ASSIGNED
shared bottleneck: room row lock + queue_order allocation
```

Restart the Spring Boot app with the `load-test` profile before running this test so the 5000 load-test users are seeded.

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

MSYS_NO_PATHCONV=1 docker run --rm \
  -p 5665:5665 \
  -v "$(pwd -W):/scripts" \
  --network grafana_default \
  -e K6_WEB_DASHBOARD=true \
  -e K6_WEB_DASHBOARD_HOST=0.0.0.0 \
  -e K6_WEB_DASHBOARD_PORT=5665 \
  grafana/k6 run -o influxdb=http://influxdb:8086/k6 /scripts/stage-speaking-queue-load.js \
  -e BASE_URL=http://sisibibi-stage-lb:8088 \
  -e ROOM_ID=1 \
  -e ROOM_COUNT=10 \
  -e USER_ID_START=1 \
  -e USERS_PER_ROOM=500 \
  -e USER_COUNT=5000 \
  -e CONTINUOUS_REQUEST_RATE=25 \
  -e CONTINUOUS_REQUEST_TIME_UNIT=1s \
  -e CONTINUOUS_DURATION=3m \
  -e CONTINUOUS_REQUEST_PRE_ALLOCATED_VUS=100 \
  -e CONTINUOUS_REQUEST_MAX_VUS=300 \
  -e CONTINUOUS_ROTATION_VUS=10 \
  -e CONTINUOUS_ROTATION_INITIAL_DELAY=20s \
  -e CONTINUOUS_ROTATION_INTERVAL_SECONDS=20 \
  -e HTTP_REQ_DURATION_P95_THRESHOLD_MS=5000 \
  -e HTTP_REQ_DURATION_P99_THRESHOLD_MS=8000 \
  -e SCENARIO=continuous_stage_operation \
  --tag testid=continuous-stage-25rps-20s-rotation-3m
```

Watch these custom metrics:

```text
stage_request_status_201
stage_request_status_409
stage_request_status_5xx
stage_current_speaker_lookup_failures
stage_continuous_turn_complete_attempts
stage_continuous_turn_complete_success
stage_continuous_turn_no_current_speaker
stage_continuous_turn_rotation_failures
```

Interpretation:

```text
201 is high, 5xx is zero, p95 rises gradually:
  RDB is preserving correctness but queue writes are waiting behind lock serialization.

409 grows during the run:
  user ids are being reused or a user already has an active request. Increase USER_COUNT or reset data.

rotation failures grow:
  complete/current-speaker transition is racing or failing under sustained writes.

no-current-speaker grows:
  some rooms have no assigned speaker at rotation time. Check whether request distribution or auto grant is working as intended.
```

### Room Opening Burst Then Operate

This scenario models the moment right after rooms open.

It assumes:

```text
rooms: 10
users per room: 100
total active user pool: 1000
opening burst: all 1000 users request speaking turn within 10s
after opening burst: no more speaking-turn requests
position polling: users intermittently check their own queue position
rotation: 1 current speaker completion per room every 20s
```

Use this to separate two different service phases:

```text
opening phase:
  request pressure is high because users apply at nearly the same time.

operation phase:
  write pressure drops, but users keep checking their position and speakers rotate.
```

Command:

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

MSYS_NO_PATHCONV=1 docker run --rm \
  -p 5665:5665 \
  -v "$(pwd -W):/scripts" \
  --network grafana_default \
  -e K6_WEB_DASHBOARD=true \
  -e K6_WEB_DASHBOARD_HOST=0.0.0.0 \
  -e K6_WEB_DASHBOARD_PORT=5665 \
  grafana/k6 run -o influxdb=http://influxdb:8086/k6 /scripts/stage-speaking-queue-load.js \
  -e BASE_URL=http://sisibibi-stage-lb:8088 \
  -e ROOM_ID=1 \
  -e ROOM_COUNT=10 \
  -e USER_ID_START=1 \
  -e USERS_PER_ROOM=100 \
  -e USER_COUNT=1000 \
  -e OPENING_BURST_REQUEST_RATE=100 \
  -e OPENING_BURST_DURATION=10s \
  -e OPENING_BURST_PRE_ALLOCATED_VUS=300 \
  -e OPENING_BURST_MAX_VUS=1000 \
  -e OPENING_OPERATION_DURATION=3m \
  -e POSITION_POLLING_VUS=20 \
  -e POSITION_POLLING_START_TIME=10s \
  -e POSITION_POLLING_SLEEP_MIN_SECONDS=1 \
  -e POSITION_POLLING_SLEEP_MAX_SECONDS=3 \
  -e CONTINUOUS_ROTATION_VUS=10 \
  -e CONTINUOUS_ROTATION_INITIAL_DELAY=20s \
  -e CONTINUOUS_ROTATION_INTERVAL_SECONDS=20 \
  -e HTTP_REQ_DURATION_P95_THRESHOLD_MS=5000 \
  -e HTTP_REQ_DURATION_P99_THRESHOLD_MS=10000 \
  -e SCENARIO=room_opening_burst_then_operate \
  --tag testid=room-opening-burst-1000users-10s-position-polling-3m
```

Watch these custom metrics:

```text
stage_request_status_201
stage_request_status_409
stage_request_status_5xx
stage_continuous_position_lookup_status_200
stage_continuous_position_lookup_status_404
stage_continuous_position_lookup_failures
stage_continuous_turn_complete_success
stage_continuous_turn_rotation_failures
```

Interpretation:

```text
opening burst p95 is high, but operation phase stabilizes:
  RDB queue is sensitive to room-opening write bursts, but ordinary operation after the burst is acceptable.

position 404 grows over time:
  users who already finished speaking no longer have an active waiting position. This is acceptable in this scenario.

request 409 grows during opening:
  duplicated user ids were reused or the queue was not reset before the run.

5xx or position failures grow:
  inspect server logs, Hikari pending connections, Tomcat busy threads, and MySQL processlist.
```

### Two App Servers Behind Nginx

Use this when you want a load-test shape closer to production.

Flow:

```text
k6 container
-> Nginx load balancer: sisibibi-stage-lb:8088
-> Spring Boot A: host.docker.internal:8080
-> Spring Boot B: host.docker.internal:8081
-> MySQL
```

Start two Spring Boot instances first.

Server A:

```text
--spring.profiles.active=load-test --server.port=8080
```

Server B:

```text
--spring.profiles.active=load-test --server.port=8081
```

Then start Nginx from the `backend/load-test/nginx` directory.

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/nginx

MSYS_NO_PATHCONV=1 docker run -d --name sisibibi-stage-lb \
  --network grafana_default \
  --ulimit nofile=65535:65535 \
  -p 8088:8088 \
  -v "$(pwd -W)/nginx.conf:/etc/nginx/nginx.conf:ro" \
  nginx:1.27-alpine
```

Check the load balancer.

```bash
curl http://localhost:8088/
docker run --rm --network grafana_default curlimages/curl:latest -i http://sisibibi-stage-lb:8088/
docker exec -it sisibibi-stage-lb nginx -T
```

`403` from Spring Security is acceptable for the root path.
The check is successful when the response is proxied to Spring Boot instead of showing the default Nginx welcome page.

Run the 500 request spike through Nginx from the k6 directory.

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

MSYS_NO_PATHCONV=1 docker run --rm \
  -p 5665:5665 \
  -v "$(pwd -W):/scripts" \
  --network grafana_default \
  -e K6_WEB_DASHBOARD=true \
  -e K6_WEB_DASHBOARD_HOST=0.0.0.0 \
  -e K6_WEB_DASHBOARD_PORT=5665 \
  -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
  -e K6_PROMETHEUS_RW_TREND_STATS=p\(90\),p\(95\),p\(99\),avg,min,max \
  grafana/k6 run -o experimental-prometheus-rw /scripts/stage-speaking-queue-load.js \
  -e BASE_URL=http://sisibibi-stage-lb:8088 \
  -e ROOM_ID=1 \
  -e ROOM_COUNT=10 \
  -e USER_ID_START=1 \
  -e USERS_PER_ROOM=50 \
  -e USER_COUNT=500 \
  -e SERVICE_CAPACITY_VUS=500 \
  -e SERVICE_CAPACITY_ITERATIONS=500 \
  -e HTTP_REQ_DURATION_P95_THRESHOLD_MS=5000 \
  -e HTTP_REQ_DURATION_P99_THRESHOLD_MS=8000 \
  -e SCENARIO=service_capacity_request_spike \
  --tag testid=service-capacity-500-spike-nginx-2app-tuned
```

Run the 1000 request spike through the same Nginx path.

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

MSYS_NO_PATHCONV=1 docker run --rm \
  -p 5665:5665 \
  -v "$(pwd -W):/scripts" \
  --network grafana_default \
  -e K6_WEB_DASHBOARD=true \
  -e K6_WEB_DASHBOARD_HOST=0.0.0.0 \
  -e K6_WEB_DASHBOARD_PORT=5665 \
  -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
  -e K6_PROMETHEUS_RW_TREND_STATS=p\(90\),p\(95\),p\(99\),avg,min,max \
  grafana/k6 run -o experimental-prometheus-rw /scripts/stage-speaking-queue-load.js \
  -e BASE_URL=http://sisibibi-stage-lb:8088 \
  -e ROOM_ID=1 \
  -e ROOM_COUNT=10 \
  -e USER_ID_START=1 \
  -e USERS_PER_ROOM=100 \
  -e USER_COUNT=1000 \
  -e SERVICE_CAPACITY_VUS=1000 \
  -e SERVICE_CAPACITY_ITERATIONS=1000 \
  -e HTTP_REQ_DURATION_P95_THRESHOLD_MS=10000 \
  -e HTTP_REQ_DURATION_P99_THRESHOLD_MS=12000 \
  -e SCENARIO=service_capacity_request_spike \
  --tag testid=service-capacity-1000-spike-nginx-2app-tuned
```

### Hikari Pool Size Baseline

`load-test` profile sets:

```yaml
spring.datasource.hikari.maximum-pool-size: 10
spring.datasource.hikari.minimum-idle: 10
spring.datasource.hikari.connection-timeout: 10000
spring.datasource.hikari.max-lifetime: 1700000
```

Restart both Spring Boot instances before running the load test.
With two app servers, the total possible DB connections become roughly:

```text
Spring Boot 8080: max 10
Spring Boot 8081: max 10
Total: max 20
```

This is an intentional fixed-size pool.
The goal is to protect MySQL from excessive concurrent DB work while preserving enough connections for the two app servers.

Use the same 500 and 1000 spike commands above with these `testid` tags:

```bash
--tag testid=service-capacity-500-spike-nginx-2app-hikari10
```

```bash
--tag testid=service-capacity-1000-spike-nginx-2app-hikari10
```

Compare these values against the previous baseline:

```text
500 baseline:  p95 around 3.95s
1000 baseline: p95 around 9.59s, p99 around 10.10s
```

During the run, also watch MySQL lock waits and process state:

```sql
SHOW FULL PROCESSLIST;

SELECT *
FROM performance_schema.data_lock_waits;

SELECT trx_id, trx_state, trx_started, trx_mysql_thread_id, trx_query
FROM information_schema.innodb_trx;
```

Interpretation:

```text
hikari10 keeps failure rate at 0% and p95 near the previous baseline:
  keep the bounded pool. The RDB queue bottleneck is lock serialization, not pool shortage.

hikari10 still shows high p95 but no 5xx:
  this is an RDB queue tail-latency limit, not a connection-pool failure.

5xx failures appear again:
  inspect server logs first, then MySQL lock wait and Hikari acquisition timeout.
```

The previous `maximum-pool-size=20` experiment produced:

```text
500 spike:
  201 success: 232
  5xx failure: 268
  http_req_failed: 53.60%
  stage_failure_rate: 53.60%
```

That result suggests that increasing the pool pushed too much concurrent work into MySQL and worsened lock contention.

The previous `maximum-pool-size=10` and `connection-timeout=3000` experiment produced:

```text
500 spike:
  201 success: 145
  5xx failure: 355
  http_req_failed: 71.00%
  stage_failure_rate: 71.00%
  root cause: Hikari connection acquisition timeout
```

The current `maximum-pool-size=10` and `connection-timeout=10000` result:

```text
500 spike:
  201 success: 500
  5xx failure: 0
  http_req_failed: 0.00%
  stage_failure_rate: 0.00%
  p95: 4.92s
  p99: 5.04s
```

This means the bounded pool protects MySQL, and the longer connection timeout converts server errors into user-visible waiting time.

The request scenario also reports status buckets in the k6 `CUSTOM` section:

```text
stage_request_status_201
stage_request_status_409
stage_request_status_other_2xx
stage_request_status_other_4xx
stage_request_status_5xx
stage_request_status_network_error
stage_request_status_other
```

Use these counters when `stage_request_failures` is greater than zero.
For example, `stage_request_status_5xx` points to server/upstream errors, while
`stage_request_status_network_error` points to connection or timeout failures before a valid HTTP response was received.

Stop Nginx after the test.

```bash
docker rm -f sisibibi-stage-lb
```

Interpretation:

```text
Nginx reduces connection refused but p95 remains high:
  app connection acceptance improved, but RDB lock serialization remains.

Nginx and two app servers significantly reduce p95:
  single-app server connection/Tomcat processing was part of the bottleneck.

Nginx run still shows MySQL LOCK WAIT on rooms ... for update:
  bottleneck converges on the shared RDB room row lock.
```

### Hot Room Queue Polling 500

This scenario creates a 500-user queue in room 1 during `setup()`, then repeatedly calls `GET /queue`.
Use it to measure the heavier read path where the server returns the whole waiting queue.

Default:

```text
setup: room 1, 250 + 250 request prefill
load: 10 VUs for 30s
target API: GET /api/v1/rooms/1/stage/queue
```

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

MSYS_NO_PATHCONV=1 docker run --rm \
  -p 5665:5665 \
  -v "$(pwd -W):/scripts" \
  --network grafana_default \
  -e K6_WEB_DASHBOARD=true \
  -e K6_WEB_DASHBOARD_HOST=0.0.0.0 \
  -e K6_WEB_DASHBOARD_PORT=5665 \
  -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
  -e K6_PROMETHEUS_RW_TREND_STATS=p\(90\),p\(95\),p\(99\),avg,min,max \
  grafana/k6 run -o experimental-prometheus-rw /scripts/stage-speaking-queue-load.js \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e ROOM_ID=1 \
  -e ROOM_COUNT=1 \
  -e USER_ID_START=1 \
  -e BATCHED_PREFILL_BATCH_SIZE=250 \
  -e BATCHED_PREFILL_BATCH_COUNT=2 \
  -e BATCHED_PREFILL_USERS_PER_ROOM=250 \
  -e HOT_ROOM_QUEUE_POLLING_VUS=10 \
  -e HOT_ROOM_QUEUE_POLLING_DURATION=30s \
  -e HTTP_REQ_DURATION_P95_THRESHOLD_MS=1000 \
  -e HTTP_REQ_DURATION_P99_THRESHOLD_MS=2000 \
  -e SCENARIO=hot_room_queue_polling_500 \
  --tag testid=hot-room-queue-polling-500-10vu-30s
```

### Expiration Candidates Run

This scenario prepares expired current speakers across multiple rooms through the load-test-only API, then calls the expiration runner once.
Use it to measure the RDB cost of candidate scan, room lock acquisition, `ASSIGNED -> EXPIRED`, and next `WAITING -> ASSIGNED`.

Default:

```text
prepare: 10 rooms, 1 expired ASSIGNED + 1 WAITING per room
run: 1 VU, 1 iteration
target API: POST /api/load-test/stage/expiration/run
```

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

MSYS_NO_PATHCONV=1 docker run --rm \
  -p 5665:5665 \
  -v "$(pwd -W):/scripts" \
  --network grafana_default \
  -e K6_WEB_DASHBOARD=true \
  -e K6_WEB_DASHBOARD_HOST=0.0.0.0 \
  -e K6_WEB_DASHBOARD_PORT=5665 \
  -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
  -e K6_PROMETHEUS_RW_TREND_STATS=p\(90\),p\(95\),p\(99\),avg,min,max \
  grafana/k6 run -o experimental-prometheus-rw /scripts/stage-speaking-queue-load.js \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e ROOM_ID=1 \
  -e USER_ID_START=1 \
  -e EXPIRATION_ROOM_COUNT=10 \
  -e EXPIRATION_WAITING_PER_ROOM=1 \
  -e EXPIRATION_RUN_VUS=1 \
  -e EXPIRATION_RUN_ITERATIONS=1 \
  -e HTTP_REQ_DURATION_P95_THRESHOLD_MS=5000 \
  -e HTTP_REQ_DURATION_P99_THRESHOLD_MS=8000 \
  -e SCENARIO=expiration_candidates_run \
  --tag testid=expiration-candidates-10-rooms-docker
```

Scale candidates by changing:

```bash
-e EXPIRATION_ROOM_COUNT=50 \
--tag testid=expiration-candidates-50-rooms-docker
```

```bash
-e EXPIRATION_ROOM_COUNT=100 \
--tag testid=expiration-candidates-100-rooms-docker
```

### Expiration Race: Complete vs Run

This scenario prepares one expired current speaker and one waiting speaker, then sends `complete` and expiration `run` at nearly the same time.
Use it to verify that only one terminal transition remains and the next speaker is assigned exactly once.

Default:

```text
setup:
  prepare one expired ASSIGNED user and one WAITING user per iteration target room
per iteration:
  race: POST /api/v1/rooms/{roomId}/stage/complete and POST /api/load-test/stage/expiration/run via http.batch
  verify: POST /api/load-test/stage/expiration/race/verify
load: 1 VU, 100 iterations
```

```bash
cd /d/NBE9-11-final-Team12/backend/load-test/k6

MSYS_NO_PATHCONV=1 docker run --rm \
  -p 5665:5665 \
  -v "$(pwd -W):/scripts" \
  --network grafana_default \
  -e K6_WEB_DASHBOARD=true \
  -e K6_WEB_DASHBOARD_HOST=0.0.0.0 \
  -e K6_WEB_DASHBOARD_PORT=5665 \
  -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
  -e K6_PROMETHEUS_RW_TREND_STATS=p\(90\),p\(95\),p\(99\),avg,min,max \
  grafana/k6 run -o experimental-prometheus-rw /scripts/stage-speaking-queue-load.js \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e ROOM_ID=1 \
  -e USER_ID_START=1 \
  -e EXPIRATION_RACE_VUS=1 \
  -e EXPIRATION_RACE_ITERATIONS=100 \
  -e HTTP_REQ_DURATION_P95_THRESHOLD_MS=5000 \
  -e HTTP_REQ_DURATION_P99_THRESHOLD_MS=8000 \
  -e SCENARIO=expiration_race_complete_vs_run \
  --tag testid=expiration-race-complete-vs-run-100
```

## Recommended Grafana Queries

Use Grafana Explore with the Prometheus datasource.

```promql
k6_http_reqs_total
```

```promql
k6_http_req_failed_rate
```

```promql
k6_http_req_duration_p95
```

```promql
k6_http_req_duration_p99
```

```promql
k6_stage_failure_rate_rate
```

```promql
k6_stage_expiration_elapsed_ms_p95
```

```promql
k6_stage_expiration_candidate_rooms_avg
```

```promql
k6_stage_expiration_expired_rooms_avg
```

Filter a specific run with:

```promql
{testid="service-capacity-100"}
```

Examples:

```promql
k6_http_req_duration_p95{testid="service-capacity-100-docker"}
```

```promql
k6_http_req_duration_p95{testid="service-capacity-200-docker"}
```

```promql
k6_http_req_duration_p95{testid="batched-prefill-500-operate-docker"}
```

```promql
k6_http_req_duration_p95{testid="hot-room-batched-500-operate-docker"}
```

```promql
k6_http_req_duration_p95{testid="hot-room-setup-prefill-500-operate-docker"}
```

```promql
k6_http_req_duration_p95{testid="hot-room-position-polling-500-50vu-30s"}
```

## Notes

- `connection refused` means k6 could not connect to Spring Boot. Do not interpret it as an RDB Queue limit.
- Use `stage_failure_rate` for scenario-level failure decisions.
- Use `http_req_duration` p95/p99 for latency observation.
