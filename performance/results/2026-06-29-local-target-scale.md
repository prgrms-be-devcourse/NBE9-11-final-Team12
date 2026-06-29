# 2026-06-29 로컬 목표 규모 성능 테스트 결과

## 1. 테스트 목적

현재 프로젝트 기준으로 다음 목표 규모를 로컬 환경에서 검증했다.

```text
토론방: 10개
방당 참여자: 100명
총 동시 접속자: 1000명
HTTP 읽기 목표: 약 300 RPS
발언권 신청 목표: 20 TPS
WebSocket 목표: 1000 연결, 약 100 chat TPS
```

## 2. 테스트 환경

| 항목 | 값 |
| --- | --- |
| Backend | Spring Boot local,monitoring profile |
| DB | MySQL Docker, localhost:23306 |
| Redis | Redis Docker, localhost:26379 |
| Load tool | k6 |
| Metrics | Prometheus, Grafana, Loki, Promtail |
| Grafana | http://localhost:3001 |
| Dashboard | Sisibibi Performance Triage |

Backend는 `local,monitoring` 프로필로 실행했다.

```bash
DB_HOST=localhost \
DB_PORT=23306 \
DB_NAME=sisibibi \
DB_USERNAME=root \
DB_PASSWORD=root \
REDIS_HOST=localhost \
REDIS_PORT=26379 \
SPRING_PROFILES_ACTIVE=local,monitoring \
SPRING_AI_OPENAI_API_KEY=dummy \
./gradlew bootRun
```

## 3. 테스트 데이터

`performance/sql/seed-performance-data.sql`로 다음 데이터를 생성했다.

| 데이터 | 규모 |
| --- | ---: |
| 사용자 | 1200명 |
| 토론방 | 10개 |
| 발언권 순번 데이터 | 10개 |
| 방 참여자 | 1000명 |
| 의견 | 500개 |
| 공감 | 10000개 |

초기 발언권 TPS 테스트에서 모든 요청이 500으로 실패했다.
원인은 성능 테스트 SQL이 `rooms`는 생성했지만 `room_queue_sequences`를 생성하지 않아 발언권 순번 발급 시 내부 오류가 발생한 것이었다.

수정 후 seed에 다음 데이터를 포함했다.

```text
room_queue_sequences(room_id, next_queue_order, created_at, updated_at)
```

## 4. HTTP 읽기 테스트

### 실행 조건

```bash
BASE_URL=http://localhost:8080 \
ROOM_IDS=900001,900002,900003,900004,900005,900006,900007,900008,900009,900010 \
USER_ID_BASE=100000 \
USER_COUNT=1000 \
RATE=30 \
DURATION=60s \
PRE_ALLOCATED_VUS=100 \
MAX_VUS=500 \
TOKEN_VERSION=0 \
k6 run performance/k6/target-scale-read.js
```

### 결과

| 지표 | 결과 |
| --- | ---: |
| 총 요청 수 | 18010 |
| 처리량 | 약 299.67 req/s |
| 실패율 | 0% |
| p95 | 14.62ms |
| p99 | 24.06ms |

상대적으로 지연 시간이 높았던 API는 다음 순서였다.

| API | p95 |
| --- | ---: |
| `GET /api/v1/users/me/trust` | 16.68ms |
| `GET /api/v1/users/{userId}/trust` | 16.47ms |
| `GET /api/v1/rooms/{roomId}/speeches` | 15.05ms |
| `GET /api/v1/rooms/{roomId}/best-speech` | 14.50ms |

현재 규모에서는 읽기 API 병목은 확인되지 않았다.
다만 신뢰도 조회는 여러 집계 쿼리를 사용하므로, 목록 화면에서 다수 사용자에 대해 반복 호출하면 병목 후보가 될 수 있다.

## 5. 발언권 신청 TPS 테스트

### 실행 조건

```bash
BASE_URL=http://localhost:8080 \
ROOM_IDS=900001,900002,900003,900004,900005,900006,900007,900008,900009,900010 \
USER_ID_BASE=100000 \
USERS_PER_ROOM=100 \
RATE=20 \
DURATION=60s \
PRE_ALLOCATED_VUS=100 \
MAX_VUS=500 \
TOKEN_VERSION=0 \
k6 run performance/k6/target-scale-stage.js
```

### 결과

| 지표 | 결과 |
| --- | ---: |
| 총 요청 수 | 1201 |
| 처리량 | 약 20 TPS |
| 서버 오류율 | 0% |
| 생성 성공 | 1010건 |
| 비즈니스 거절 | 191건 |
| p95 | 52.78ms |
| p99 | 65.69ms |
| max | 190.84ms |

비즈니스 거절은 중복 신청, 이미 배정된 발언권 등 정책상 정상적으로 발생 가능한 응답이다.
k6 스크립트는 `400`, `403`, `409`를 서버 실패로 보지 않고 비즈니스 거절로 분리한다.

현재 20 TPS에서는 DB 커넥션 대기나 서버 오류가 확인되지 않았다.

## 6. WebSocket 1000 연결 테스트

### 실행 조건

```bash
BASE_URL=http://localhost:8080 \
ROOM_IDS=900001,900002,900003,900004,900005,900006,900007,900008,900009,900010 \
USER_ID_BASE=100000 \
USERS_PER_ROOM=100 \
VUS=1000 \
MAX_DURATION=100s \
CONNECTION_DURATION_SECONDS=45 \
MESSAGE_INTERVAL_SECONDS=10 \
TOKEN_VERSION=0 \
k6 run performance/k6/target-scale-websocket.js
```

### 결과

| 지표 | 결과 |
| --- | ---: |
| WebSocket 연결 | 1000/1000 성공 |
| STOMP CONNECT | 1000건 |
| SUBSCRIBE | 1000건 |
| SEND | 4000건 |
| 브로드캐스트 수신 | 314755건 |
| 실패율 | 0% |
| 연결 p95 | 1.58s |
| 세션 유지 | 약 45초 |

검증 항목은 다음과 같다.

- WebSocket upgrade 성공
- STOMP CONNECT 성공
- 방별 채팅 topic 구독 성공
- 메시지 전송 성공
- 브로드캐스트 수신 성공

현재 로컬 환경에서 1000 연결은 통과했다.
다만 1000 연결을 한 번에 생성하므로 연결 p95가 1.58초까지 상승했다.
운영 테스트에서는 ramp-up 방식으로 실제 유입 패턴을 나눠 검증해야 한다.

## 7. Prometheus 지표 요약

테스트 직후 Prometheus에서 5분 구간 기준으로 확인한 주요 지표다.

| 지표 | 값 |
| --- | ---: |
| HTTP RPS max | 20.27 RPS |
| 발언권 API p95 max | 53.5ms |
| HTTP 5xx RPS max | 0 |
| Hikari active max | 1 |
| Hikari pending max | 0 |
| JVM live threads max | 239 |
| Redis ops max | 약 225 ops/s |
| MySQL threads connected max | 12 |
| MySQL slow query 증가 | 0 |
| Host CPU 사용률 max | 약 4.9% |
| Host memory 사용률 max | 약 24.3% |

Prometheus 조회 시점이 발언권 테스트 직후라 HTTP RPS는 발언권 테스트 구간 기준이다.
HTTP 읽기 300 RPS는 k6 결과 기준으로 별도 기록했다.

## 8. 병목 판단

현재 목표 규모 테스트에서는 명확한 병목은 확인되지 않았다.

확인 근거는 다음과 같다.

- HTTP 읽기 300 RPS 실패율 0%
- 발언권 20 TPS 서버 오류율 0%
- WebSocket 1000 연결 실패율 0%
- Hikari pending 0
- MySQL slow query 증가 0
- Redis ops 처리 정상
- Host CPU와 memory 여유 있음

다만 다음 영역은 이후 고도화 후보로 남긴다.

| 후보 | 이유 | 대응 방향 |
| --- | --- | --- |
| 발언권 신청 | 방 단위 순번 발급과 상태 변경이 직렬화될 수 있음 | 50 TPS 이상 stress 테스트, room별 lock 대기 확인 |
| 신뢰도 조회 | 여러 집계 쿼리 기반 | 목록 반복 호출 시 캐시 또는 스냅샷 검토 |
| WebSocket fan-out | 1000명 기준 브로드캐스트 수신량이 급증 | 방별 메시지 TPS 증가 테스트, broker relay 검토 |
| SQL 로그 | local 프로필에서 Hibernate SQL 로그가 많음 | 성능 테스트 전용 프로필에서는 SQL 로그 비활성화 |
| 스케줄러 | 발언권/AI 리포트/요약 스케줄러가 테스트 중 같이 동작 | 병목 분석 시 스케줄러 on/off 비교 |

## 9. 다음 테스트 계획

다음 단계에서는 부하를 더 높여 병목 지점을 의도적으로 찾아야 한다.

1. HTTP 읽기 `RATE=50~100`으로 500~1000 RPS stress 테스트
2. 발언권 신청 `RATE=30~50`으로 lock 대기와 p95 증가 확인
3. WebSocket `MESSAGE_INTERVAL_SECONDS=5`로 채팅 TPS 200 수준 검증
4. 운영 서버에서는 ramp-up, soak, spike 테스트를 분리
5. Grafana Cloud 또는 별도 모니터링 서버로 운영 지표 수집 구조 확장


## 10. 발언권 추가 Stress 테스트

기존 `20 TPS × 60초`는 목표 규모 기준 load 테스트다.
발언권 신청은 사용자가 지속적으로 초당 수십 번 호출하는 API가 아니라, 토론 진행 중 특정 시점에 몰리는 burst 성격의 쓰기 API다.
따라서 20 TPS는 기준 부하 검증으로 보고, 추가로 30 TPS와 50 TPS를 stress 성격으로 검증했다.

### 30 TPS 결과

```bash
RATE=30 DURATION=30s k6 run performance/k6/target-scale-stage.js
```

| 지표 | 결과 |
| --- | ---: |
| 총 요청 수 | 901 |
| 생성 성공 | 901 |
| 실패율 | 0% |
| p95 | 40.59ms |
| p99 | 48.73ms |
| max | 101.93ms |

### 50 TPS 결과

```bash
RATE=50 DURATION=20s k6 run performance/k6/target-scale-stage.js
```

| 지표 | 결과 |
| --- | ---: |
| 총 요청 수 | 1001 |
| 생성 성공 | 1000 |
| 비즈니스 거절 | 1 |
| 서버 오류율 | 0% |
| p95 | 68.92ms |
| p99 | 422.84ms |
| max | 552.28ms |

50 TPS에서는 p95는 안정적이지만 p99와 max가 튀었다.
Prometheus 기준으로는 다음 문제가 확인되지 않았다.

- HTTP 5xx 증가 없음
- Hikari pending 0
- MySQL slow query 증가 없음
- Host CPU 여유 있음

따라서 현재 관측 기준 병목은 DB 커넥션 고갈이나 slow query라기보다, 방 단위 발언권 순번 발급과 상태 변경이 순간적으로 직렬화되는 구간에서 일부 tail latency가 발생한 것으로 판단한다.

### 결론

- 목표 기준인 20 TPS는 통과함
- 30 TPS도 안정적으로 통과함
- 50 TPS도 서버 오류 없이 통과했지만 p99 tail latency가 상승함
- 다음 성능 개선은 평균 응답이 아니라 p99 tail latency를 줄이는 방향으로 진행해야 함

### 다음 확인 대상

1. `room_queue_sequences` 비관적 락 대기 시간 측정
2. 같은 방 집중 부하와 10개 방 분산 부하 비교
3. Hibernate SQL 로그 비활성화 후 재측정
4. 발언권 신청 트랜잭션 내부 쿼리 수 축소 가능성 검토
5. 운영 서버에서는 ramp-up 방식으로 50 TPS 이상 재검증
