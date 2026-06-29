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

## 11. 목표 기준 산정 근거

이번 목표는 다음 서비스 가정을 기준으로 잡았다.

```text
토론방 수: 10개
방당 동시 접속자: 50~100명
총 동시 접속자: 500~1000명
```

동시 접속자 수가 그대로 RPS가 되는 것은 아니다.
RPS/TPS는 사용자가 몇 초마다 어떤 행동을 하는지로 환산한다.

| 사용자 행동 | 산정 방식 | 목표값 |
| --- | --- | ---: |
| 화면 상태 조회 | 1000명이 10~30초마다 조회 | 약 30~100 RPS |
| 의견 목록·상세 조회 | 1000명이 10~20초마다 갱신 | 약 50~100 RPS |
| 보조 조회 | 내 정보, 제재, 신뢰도 등을 30~60초마다 조회 | 약 10~30 RPS |
| HTTP 읽기 혼합 | 여러 조회 API를 한 iteration에 묶어 호출 | 약 300 RPS |
| 채팅 | 방당 1~3 msg/s | 약 10~30 TPS |
| 발언권 신청 | 특정 시점에 몰리는 burst 요청 | 약 10~50 TPS |
| WebSocket | 접속자 수와 동일 | 500~1000 연결 |

따라서 이번 테스트의 기준은 다음과 같이 해석한다.

- `300 RPS` 읽기 테스트는 1000명 규모에서 여러 조회 API가 동시에 섞이는 상황을 가정한 load 테스트다.
- `20 TPS` 발언권 테스트는 목표 운영 부하 기준의 load 테스트다.
- `30~50 TPS` 발언권 테스트는 순간 집중 요청을 보는 stress 테스트다.
- `1000 WebSocket` 테스트는 목표 동시 접속 상한 검증이다.

## 12. 성능 테스트와 동시성 테스트 구분

이번 테스트는 성능 테스트와 동시성 테스트가 섞여 있다.

성능 테스트는 다음을 본다.

- 목표 RPS/TPS를 처리할 수 있는가
- p95/p99 응답 시간이 허용 가능한가
- DB 커넥션, Redis, JVM, CPU, 메모리에 병목이 있는가

동시성 테스트는 다음을 본다.

- 동시에 요청해도 정원, 중복, 상태 전이 규칙이 깨지지 않는가
- DB unique constraint, 비관적 락, Redis Lua/script 기반 처리가 실제로 동작하는가
- 성공해야 할 요청과 거절되어야 할 요청이 정책대로 나뉘는가

즉 `발언권 신청 TPS`는 성능 테스트이면서 동시에 동시성 테스트다.
발언권 신청은 같은 방의 순번 발급과 현재 발언자 배정이 얽혀 있으므로 tail latency와 정합성을 함께 봐야 한다.

## 13. 추가 동시성 불변식 테스트

최신 dev 반영 후 다음 경합 시나리오를 추가로 검증했다.

### 13.1 토론방 정원 초과 입장

```bash
ROOM_ID=900001 \
USER_ID_BASE=101000 \
ATTEMPTS=20 \
ROOM_CAPACITY=100 \
KNOWN_EXISTING_PARTICIPANTS=100 \
k6 run performance/k6/room-join-capacity.js
```

| 항목 | 결과 |
| --- | ---: |
| 기존 참여자 | 100 |
| 정원 | 100 |
| 동시 입장 시도 | 20 |
| 추가 입장 성공 | 0 |
| 정원 불변식 | 유지 |

### 13.2 같은 사용자의 동일 의견 공감 중복 생성

```bash
SPEECH_ID=22 \
USER_ID=100030 \
ATTEMPTS=20 \
k6 run performance/k6/reaction-race.js
```

| 항목 | 결과 |
| --- | ---: |
| 동시 공감 요청 | 20 |
| 생성된 공감 | 1 |
| 중복 생성 방어 | 유지 |
| DB 확인 | `speech_reactions` 1건 |

### 13.3 같은 사용자의 동일 의견 중복 신고

```bash
SPEECH_ID=22 \
USER_ID=100030 \
ATTEMPTS=20 \
k6 run performance/k6/speech-report-race.js
```

| 항목 | 결과 |
| --- | ---: |
| 동시 신고 요청 | 20 |
| 생성된 신고 | 1 |
| 중복 생성 방어 | 유지 |
| DB 확인 | `speech_reports` 1건 |

## 14. 추가로 동시성 고려가 필요한 영역

이번에 검증한 영역 외에도 다음은 별도 시나리오로 확장할 수 있다.

| 영역 | 위험 | 검증 방향 |
| --- | --- | --- |
| 발언 종료와 자동 만료 | 동시에 완료 처리되면 다음 발언자 중복 배정 가능 | 완료/만료 동시 요청 경합 |
| 발언권 신청 취소와 자동 배정 | 취소 중인 사용자가 배정될 가능성 | 취소/배정 동시 경합 |
| 신고 처리 | 두 관리자가 같은 신고를 동시에 처리 | `findByIdForUpdate` 기반 상태 전이 검증 |
| 사용자 제재 등록/해제 | 중복 제재, 해제와 연장 경합 | 동일 사용자 제재 동시 처리 |
| 채팅 rate limiter | Redis 장애 또는 동시 증가 시 제한 누락 | Redis 카운터 및 fail-open 정책 검증 |
| WebSocket presence | 접속/해제 이벤트 순서 꼬임 | Redis presence expiration 검증 |
| Outbox relay | 다중 인스턴스에서 같은 이벤트 중복 발행 | relay lock 및 deduplication 검증 |

MVP 이후 안정화에서는 위 항목을 기능별로 나눠 k6 또는 통합 테스트로 추가하는 것이 적절하다.

## 15. 의도적 고부하 재측정

목표 기준의 여유 한계를 보기 위해 기존 목표보다 부하를 높여 재측정했다.

### 15.1 읽기 혼합 500 iteration/s

```bash
RATE=500 \
DURATION=30s \
PRE_ALLOCATED_VUS=200 \
MAX_VUS=1000 \
k6 run performance/k6/target-scale-read.js
```

`target-scale-read.js`는 한 iteration에서 주요 조회 API 10개를 호출한다.
따라서 `RATE=500`은 실제 HTTP 요청 기준 약 `500 × 10 = 5000 req/s`를 목표로 시도한 것이다.

| 항목 | 결과 |
| --- | ---: |
| 실제 HTTP 요청 수 | 35,720 |
| 실제 평균 HTTP RPS | 약 991 req/s |
| HTTP 실패율 | 0% |
| p95 | 1.23s |
| p99 | 1.39s |
| dropped iterations | 11,428 |

엔드포인트별 p95는 대부분 1.1~1.3초 구간으로 함께 상승했다.

| API | p95 |
| --- | ---: |
| `GET /api/v1/rooms/{roomId}/best-speech` | 1.31s |
| `GET /api/v1/users/{userId}/trust` | 1.28s |
| `GET /api/v1/rooms/{roomId}/stage/requests/me` | 1.27s |
| `GET /api/v1/rooms/{roomId}/stage` | 1.21s |
| `GET /api/v1/rooms/open` | 1.18s |

실행 중 Prometheus 기준:

| 지표 | 값 |
| --- | ---: |
| HTTP 5xx | 없음 |
| Hikari active | 1 |
| Hikari pending | 0 |
| MySQL threads connected | 11 |
| MySQL slow query rate | 0 |
| Redis ops/sec | 약 13.4 |
| JVM live threads | 약 246 |
| system CPU | 약 60% |

판단:

- DB 커넥션 대기, slow query, Redis 병목은 확인되지 않았다.
- 특정 API 하나만 느려진 것이 아니라 조회 API 전반의 p95가 같이 상승했다.
- k6가 `MAX_VUS=1000`에 도달했고 dropped iteration이 발생했으므로, 목표 arrival rate를 서버와 클라이언트가 끝까지 유지하지 못했다.
- 현재 병목 후보는 단일 쿼리보다 로컬 환경의 HTTP 처리량, Tomcat worker/thread 증가, 인증 필터와 다수 조회 API 혼합 처리 비용이다.

다음 개선 방향:

1. 운영 서버 또는 별도 부하 발생기에서 동일 테스트 재측정
2. 조회 혼합 테스트를 API별 단독 테스트로 쪼개 가장 비싼 조회 식별
3. 인증 검증 비용과 신뢰도 조회 집계 비용 분리 측정
4. `GET /api/v1/users/{userId}/trust`, `GET /api/v1/rooms/{roomId}/best-speech` 캐시 또는 스냅샷 필요성 검토
5. Grafana에서 Tomcat thread, JVM thread, CPU, HTTP duration을 같은 시간축으로 비교

### 15.2 발언권 신청 80 TPS

```bash
RATE=80 \
DURATION=20s \
k6 run performance/k6/target-scale-stage.js
```

| 항목 | 결과 |
| --- | ---: |
| 전체 요청 | 1,601 |
| 생성 성공 | 1,000 |
| 비즈니스 거절 | 601 |
| 서버 실패율 | 0% |
| p95 | 18.46ms |
| p99 | 28.59ms |
| max | 75.12ms |

비즈니스 거절은 성능 실패가 아니라 테스트 데이터 범위 내에서 이미 발언권 신청이 생성된 사용자에 대한 중복/불가 요청이다.
80 TPS에서도 서버 오류와 응답 지연은 발생하지 않았다.

판단:

- 발언권 신청은 현재 로컬 기준 80 TPS까지 큰 병목이 확인되지 않았다.
- 이전 50 TPS 테스트에서 p99가 튄 것은 순간 경합 또는 로컬 환경 편차로 보이며, 반복 측정이 필요하다.
- 더 정확한 한계 측정을 하려면 사용자 수를 더 늘리고 100~200 TPS ramp-up 테스트를 별도로 수행해야 한다.

## 16. 한계 탐색 추가 측정

운영 예상 부하를 보수적으로 잡는 것과 별개로, Redis projection과 발언권 구조의 근거를 확인하려면 더 높은 부하에서 한계 신호를 확인해야 한다.
따라서 발언권 신청은 TPS 기준 테스트와 별도로 동시 burst 테스트를 추가로 수행했다.

### 16.1 발언권 15개 방 × 100명 동시 신청 burst

```bash
RUN_SCENARIOS=multi-15rooms-100 \
bash performance/k6/run-stage-request-scenarios.sh
```

| 항목 | 결과 |
| --- | ---: |
| 방 수 | 15 |
| 방당 신청자 | 100 |
| 총 신청자 | 1,500 |
| 생성 성공 | 1,500 |
| 비즈니스 거절 | 0 |
| DB 저장 건수 | 1,500 |
| Redis 대기열 건수 | 1,500 |
| 평균 응답 시간 | 1,566.56ms |
| p95 | 2,909.10ms |
| p99 | 2,996.57ms |
| 처리량 | 476.91 req/s |
| HTTP 실패율 | 0% |
| checks | 100% |

판단:

- 정합성은 유지됐다. DB와 Redis 대기열 모두 1,500건으로 맞았다.
- 서버 오류는 없었지만 p95가 약 2.9초까지 상승했다.
- 이 시나리오는 “평균 운영 부하”가 아니라 토론 시작 직후 사용자가 동시에 발언권을 신청하는 burst 상황이다.
- 발언권 신청의 한계 신호는 실패율보다 먼저 tail latency로 나타난다.
- Redis projection은 대기열 조회와 실시간 상태 제공에는 유리하지만, 신청 저장 경로 자체는 여전히 DB insert, 순번 발급, 트랜잭션 경합 영향을 받는다.

운영 목표 재조정:

| 구분 | 기존 보수 기준 | 조정 가능 기준 | 의미 |
| --- | ---: | ---: | --- |
| 발언권 운영 예상 부하 | 20~50 TPS | 50~100 TPS | 일반 운영 목표 |
| 발언권 burst 검증 | 50 TPS | 300~500 req/s | 토론 시작 직후 집중 요청 |
| 발언권 한계 탐색 | 미정 | 500 req/s 이상 | tail latency와 경합 확인 |

### 16.2 WebSocket 1,200 연결 시도

```bash
VUS=1200 \
USERS_PER_ROOM=120 \
CONNECTION_DURATION_SECONDS=30 \
MESSAGE_INTERVAL_SECONDS=10 \
k6 run performance/k6/target-scale-websocket.js
```

| 항목 | 결과 |
| --- | ---: |
| WebSocket upgrade | 1,200 |
| STOMP CONNECT | 1,200 |
| SUBSCRIBE | 1,200 |
| connect p95 | 812.05ms |
| WebSocket failure rate | 42.85% |
| 메시지 전송 성공 체크 | 300 / 1,200 |
| 브로드캐스트 수신 성공 체크 | 300 / 1,200 |

판단:

- 연결과 구독 자체는 1,200개까지 성공했다.
- 메시지 전송/수신 검증은 실패율이 높았다.
- 현재 seed 데이터는 방당 100명 참여자를 기준으로 구성되어 있는데, 테스트는 방당 120명으로 실행했다. 따라서 일부 사용자는 실제 참여자 조건과 맞지 않을 수 있다.
- 이 결과는 “1,200 연결 불가”라기보다 “현재 테스트 데이터/스크립트 조건으로는 1,200명 채팅 송수신 검증이 깨진다”로 해석해야 한다.
- WebSocket 한계 측정은 방당 120명 이상의 참여자 seed를 별도로 만든 뒤 재측정해야 한다.

## 17. 현재 병목 후보 우선순위

| 우선순위 | 영역 | 근거 | 다음 액션 |
| --- | --- | --- | --- |
| 1 | 읽기 혼합 부하 | 약 1,000 RPS에서 p95 1초 초과, dropped iteration 발생 | API별 단독 부하로 비싼 조회 분리 |
| 2 | 발언권 burst | 1,500명 동시 신청에서 p95 약 2.9초 | 300/500/800 req/s rate 테스트와 DB lock 지표 확인 |
| 3 | WebSocket 채팅 송수신 | 1,200 연결은 성공, 송수신 검증 실패 | 1,200명 참여자 seed 보강 후 재측정 |
| 4 | 신뢰도/베스트 의견 조회 | 고부하 읽기에서 p95 상위권 | 캐시 또는 스냅샷 검토 후보 |

현재 운영 예상 부하는 기존보다 높게 잡을 수 있다.
다만 문서에는 “운영 목표”와 “한계 탐색”을 분리해서 적어야 한다.

```text
운영 목표 = 안정적으로 감당해야 하는 기준
한계 탐색 = 어디서 tail latency, 실패율, dropped iteration이 발생하는지 찾는 기준
```

## 18. 한계 탐색 재측정

사용자가 지정한 고정 규모에 맞추기보다, 현재 로컬 환경에서 의미 있는 한계 신호가 어디서 나타나는지 확인했다.
발언권은 TPS 기반 rate 테스트와 순수 동시 burst 테스트를 분리했고, WebSocket은 같은 1,000명 연결에서 메시지 주기를 줄이며 한계 구간을 확인했다.

### 18.1 발언권 신청 rate 테스트

#### 200 TPS

```bash
RATE=200 \
DURATION=10s \
ROOM_COUNT=10 \
USER_ID_BASE=100000 \
k6 run performance/k6/target-scale-stage.js
```

| 항목 | 결과 |
| --- | ---: |
| 전체 요청 | 2,000 |
| 생성 성공 | 1,000 |
| 비즈니스 거절 | 1,000 |
| 서버 실패율 | 0% |
| p95 | 16.13ms |
| p99 | 34.69ms |
| 처리량 | 199.95 req/s |

#### 500 TPS

```bash
RATE=500 \
DURATION=10s \
ROOM_COUNT=10 \
USER_ID_BASE=100000 \
k6 run performance/k6/target-scale-stage.js
```

| 항목 | 결과 |
| --- | ---: |
| 전체 요청 | 5,001 |
| 생성 성공 | 1,000 |
| 비즈니스 거절 | 4,001 |
| 서버 실패율 | 0% |
| p95 | 83.60ms |
| p99 | 178.74ms |
| 처리량 | 499.63 req/s |

판단:

- 200 TPS와 500 TPS 모두 서버 실패는 없었다.
- 다만 seed 데이터가 1,000명의 유효 사용자 기준이라, 1,000건 생성 이후에는 중복 신청 등 비즈니스 거절이 포함된다.
- 따라서 이 결과는 “500 TPS까지 요청 처리 경로가 무너지지 않는다”는 의미이며, “500 TPS 순수 생성 성공”을 의미하지는 않는다.
- 순수 생성 한계는 아래 burst 테스트로 별도 확인했다.

### 18.2 발언권 30개 방 × 100명 동시 신청 burst

```bash
RUN_SCENARIOS=multi-15rooms-100 \
MULTI_ROOM_COUNT=30 \
bash performance/k6/run-stage-request-scenarios.sh
```

| 항목 | 결과 |
| --- | ---: |
| 방 수 | 30 |
| 방당 신청자 | 100 |
| 총 신청자 | 3,000 |
| 생성 성공 | 3,000 |
| 비즈니스 거절 | 0 |
| DB 저장 건수 | 3,000 |
| Redis 대기열 건수 | 3,000 |
| 평균 응답 시간 | 2,266.89ms |
| p95 | 4,467.78ms |
| p99 | 4,647.38ms |
| 처리량 | 606.74 req/s |
| HTTP 실패율 | 0% |
| checks | 100% |

판단:

- 정합성은 유지됐다. DB 저장 건수와 Redis 대기열 건수가 모두 3,000건으로 일치했다.
- 서버 오류는 없었지만 p95가 약 4.47초까지 상승했다.
- 발언권 신청의 한계 신호는 실패율보다 tail latency로 먼저 나타난다.
- 현재 구조에서 발언권 신청 저장 경로는 RDB 원본 저장, 순번 확정, Redis projection 반영을 거치므로 burst 상황에서는 DB insert/트랜잭션/Redis 동기화 비용이 누적된다.
- 운영 부하 목표와 한계 탐색 목표는 분리해서 봐야 한다.

| 구분 | 현재 판단 |
| --- | --- |
| 일반 운영 목표 | 100~200 TPS 수준은 안정권으로 볼 수 있음 |
| 고부하 검증 목표 | 500 TPS 이상에서 tail latency 확인 필요 |
| burst 한계 신호 | 3,000명 동시 신청 시 p95 4초대 |
| 개선 후보 | 신청 저장 경로 지표 분리, DB insert/락 대기/Redis 반영 시간 계측 |

### 18.3 WebSocket 1,000명 메시지 주기별 테스트

WebSocket은 연결 수만 보는 것이 아니라 STOMP CONNECT, SUBSCRIBE, SEND, 브로드캐스트 수신까지 확인했다.
같은 1,000명 기준에서 메시지 전송 주기를 5초, 2초, 1초로 줄이며 한계 구간을 확인했다.

#### 1,000명 / 5초마다 메시지 전송

```bash
VUS=1000 \
CONNECTION_DURATION_SECONDS=30 \
MESSAGE_INTERVAL_SECONDS=5 \
k6 run performance/k6/target-scale-websocket.js
```

| 항목 | 결과 |
| --- | ---: |
| 연결 성공 | 1,000 |
| STOMP CONNECT | 1,000 |
| SUBSCRIBE | 1,000 |
| WebSocket failure rate | 0% |
| connect p95 | 774ms |
| 애플리케이션 메시지 전송률 | 약 170.97 msg/s |
| 브로드캐스트 수신률 | 약 6,494.76 msg/s |
| checks | 100% |

#### 1,000명 / 2초마다 메시지 전송

```bash
VUS=1000 \
CONNECTION_DURATION_SECONDS=30 \
MESSAGE_INTERVAL_SECONDS=2 \
k6 run performance/k6/target-scale-websocket.js
```

| 항목 | 결과 |
| --- | ---: |
| 연결 성공 | 1,000 |
| STOMP CONNECT | 1,000 |
| SUBSCRIBE | 1,000 |
| WebSocket failure rate | 0% |
| connect p95 | 509.04ms |
| 애플리케이션 메시지 전송률 | 약 466.53 msg/s |
| 브로드캐스트 수신률 | 약 3,650.81 msg/s |
| checks | 100% |

#### 1,000명 / 1초마다 메시지 전송

```bash
VUS=1000 \
CONNECTION_DURATION_SECONDS=20 \
MESSAGE_INTERVAL_SECONDS=1 \
k6 run performance/k6/target-scale-websocket.js
```

| 항목 | 결과 |
| --- | ---: |
| WebSocket upgrade | 1,000 |
| STOMP CONNECT | 1,000 |
| SUBSCRIBE | 1,000 |
| WebSocket failure rate | 7.91% |
| connect p95 | 18.92s |
| 메시지 전송 성공 체크 | 914 / 1,000 |
| 브로드캐스트 수신 성공 체크 | 821 / 1,000 |
| 애플리케이션 메시지 전송률 | 약 56.65 msg/s |
| 브로드캐스트 수신률 | 약 84 msg/s |

판단:

- 1,000명 연결 자체는 가능하다.
- 1,000명이 5초 또는 2초마다 채팅을 보내는 수준은 로컬 기준 통과했다.
- 1,000명이 1초마다 채팅을 보내는 수준에서는 실패율과 connect p95가 급증했다.
- 현재 WebSocket 한계 신호는 단순 연결 수보다 메시지 처리량과 브로드캐스트 처리량에서 먼저 나타난다.
- 운영 목표는 1,000명 동접 기준 2~5초 메시지 주기를 기준으로 잡고, 1초 주기 수준을 요구하려면 메시지 브로커, 브로드캐스트 최적화, 저장 경로 분리 등을 검토해야 한다.

### 18.4 최종 해석

| 영역 | 한계 신호 | 현재 판단 | 다음 개선 후보 |
| --- | --- | --- | --- |
| 발언권 신청 | burst p95 4초대 | 정합성은 유지, tail latency가 먼저 증가 | DB/Redis 단계별 시간 계측, 순번 발급 경로 분석 |
| WebSocket 채팅 | 1초 주기에서 실패율 증가 | 1,000명 연결은 가능, 초고빈도 송수신은 병목 | 브로커 릴레이, 메시지 저장/브로드캐스트 분리, 백프레셔 정책 |
| 읽기 혼합 API | 약 1,000 RPS에서 p95 1초 초과 | API별 병목 분리 필요 | 신뢰도/베스트 의견/목록 조회 단독 테스트 |

현재 로컬 기준으로는 “서버가 바로 실패하는 지점”보다 “tail latency가 급격히 증가하는 지점”이 먼저 확인됐다.
따라서 다음 성능 개선은 실패율만 보는 것이 아니라 p95/p99, Tomcat thread, DB connection, JVM CPU, Redis latency를 같은 시간축에서 같이 봐야 한다.

## 19. REST + WebSocket 혼합 부하 테스트

단독 API 한계가 아니라 실제 서비스처럼 HTTP 조회/쓰기/발언권 신청과 WebSocket 채팅이 동시에 발생하는 상황을 확인했다.
혼합 부하에서는 같은 Spring Boot 인스턴스가 HTTP 요청, WebSocket 세션, STOMP 메시지 처리, 채팅 저장, DB/Redis 접근을 함께 처리하므로 단독 테스트보다 병목이 더 빨리 드러날 수 있다.

### 19.1 혼합 부하 스크립트

```bash
k6 run performance/k6/target-scale-mixed-limit.js
```

동시에 실행한 작업:

| 시나리오 | 내용 |
| --- | --- |
| `readApis` | 내 정보, 토론방 목록/상세, 참여자 수, 의견 목록, 발언권 상태, 베스트 의견, 신뢰도 조회 |
| `writeApis` | 공감 등록/취소, 의견 신고 |
| `stageRequests` | 발언권 신청 |
| `websocketChat` | WebSocket 연결, STOMP CONNECT, SUBSCRIBE, SEND, 브로드캐스트 수신 |

### 19.2 혼합 부하 1차: HTTP 350 RPS 목표 + WebSocket 500명

```bash
READ_RATE=200 \
WRITE_RATE=50 \
STAGE_RATE=100 \
WS_VUS=500 \
MESSAGE_INTERVAL_SECONDS=5 \
DURATION=30s \
k6 run performance/k6/target-scale-mixed-limit.js
```

| 항목 | 결과 |
| --- | ---: |
| HTTP 요청 수 | 35,120 |
| HTTP 처리량 | 1,063.71 req/s |
| HTTP 실패율 | 0% |
| HTTP p95 | 881.62ms |
| HTTP p99 | 1.05s |
| dropped iterations | 2,775 |
| WebSocket 연결 수 | 500 |
| WebSocket connect p95 | 425.04ms |
| WebSocket 메시지 전송 | 2,646 |
| WebSocket 메시지 수신 | 101 |
| WebSocket failure rate | 79.80% |

판단:

- HTTP 자체는 5xx 없이 처리됐지만, k6가 목표 도착률을 유지하지 못해 dropped iteration이 크게 발생했다.
- WebSocket 연결과 STOMP 연결은 가능했지만 브로드캐스트 수신이 급격히 줄었다.
- 이 조건에서는 HTTP와 WebSocket을 동시에 처리할 때 채팅 브로드캐스트/수신 경로가 먼저 깨진다.

### 19.3 혼합 부하 2차: HTTP 175 RPS 목표 + WebSocket 500명

```bash
READ_RATE=100 \
WRITE_RATE=25 \
STAGE_RATE=50 \
WS_VUS=500 \
MESSAGE_INTERVAL_SECONDS=5 \
DURATION=30s \
k6 run performance/k6/target-scale-mixed-limit.js
```

| 항목 | 결과 |
| --- | ---: |
| HTTP 요청 수 | 28,977 |
| HTTP 처리량 | 913.58 req/s |
| HTTP 실패율 | 0% |
| HTTP p95 | 344.96ms |
| HTTP p99 | 543.34ms |
| dropped iterations | 114 |
| WebSocket 연결 수 | 500 |
| WebSocket connect p95 | 458ms |
| WebSocket 메시지 전송 | 2,400 |
| WebSocket 메시지 수신 | 1,498 |
| WebSocket failure rate | 34.18% |

판단:

- HTTP p95는 안정권으로 내려갔고 dropped iteration도 크게 줄었다.
- 하지만 WebSocket 수신 실패는 여전히 남았다.
- 단독 WebSocket 기준선과 비교하면 혼합 부하가 WebSocket 브로드캐스트 처리에 직접적인 영향을 준다.

### 19.4 WebSocket 단독 기준선: 500명 / 5초 주기

```bash
VUS=500 \
MESSAGE_INTERVAL_SECONDS=5 \
CONNECTION_DURATION_SECONDS=30 \
k6 run performance/k6/target-scale-websocket.js
```

| 항목 | 결과 |
| --- | ---: |
| WebSocket 연결 수 | 500 |
| STOMP CONNECT | 500 |
| SUBSCRIBE | 500 |
| WebSocket failure rate | 0% |
| connect p95 | 254ms |
| 메시지 전송 | 2,646 |
| 메시지 수신 | 125,013 |
| checks | 100% |

판단:

- 같은 500명/5초 조건에서 WebSocket 단독 테스트는 정상 통과했다.
- 따라서 WebSocket 자체 연결 수 한계라기보다, REST 부하와 동시에 발생할 때 브로드캐스트 처리량이 감소하는 혼합 부하 병목으로 보는 것이 타당하다.

### 19.5 현재 병목 해석

| 병목 후보 | 근거 | 우선순위 |
| --- | --- | ---: |
| WebSocket 브로드캐스트 처리 경로 | 단독 500명은 통과, 혼합 부하에서 수신 실패율 34~79% | 1 |
| HTTP 조회 시나리오의 누적 비용 | 읽기 시나리오가 iteration당 여러 API를 직렬 호출하며 dropped iteration 발생 | 2 |
| DB connection/transaction 경합 | 채팅 저장, 신고, 공감, 발언권 신청이 동시에 DB 사용 | 3 |
| 단일 애플리케이션 인스턴스 자원 경합 | HTTP와 WebSocket이 같은 JVM/스레드/DB pool 공유 | 4 |

결론:

- 현재 로컬 기준 실제 병목은 “HTTP API 5xx”가 아니라 “혼합 부하에서 WebSocket 메시지 수신이 밀리는 현상”으로 먼저 나타났다.
- WebSocket 단독 결과만으로는 운영 안정성을 판단하면 안 된다.
- 다음 개선은 WebSocket 메시지 저장/브로드캐스트 경로의 계측과 분리가 우선이다.

개선 후보:

1. WebSocket 메시지 처리 시간, DB 저장 시간, 브로드캐스트 시간 로그/메트릭 추가
2. 채팅 저장과 브로드캐스트의 트랜잭션 경계 확인
3. 단순 브로커 대신 외부 broker relay 검토
4. 채팅 저장 실패/지연 시 브로드캐스트 정책 결정
5. HTTP 읽기 시나리오를 API별 단독 테스트로 분해해 가장 비싼 조회 분리
6. 운영 서버에서는 부하 발생기와 애플리케이션 서버를 분리해 재측정

## 20. 혼합 부하 재측정 및 병목 원인 정정

19장 측정 이후 테스트 데이터와 스크립트를 재확인한 결과, 초기 혼합 부하 스크립트가 `SPEECH_ID_BASE=1`을 기본값으로 사용하고 있었다.
하지만 성능 테스트 seed는 의견을 auto increment로 생성하고 cleanup 시 auto increment를 초기화하지 않았다.
따라서 반복 테스트 후에는 실제 성능 테스트 의견 ID가 `1~500`이 아니었고, 일부 write API가 존재하지 않는 의견을 대상으로 요청될 수 있었다.

이를 보정하기 위해 성능 테스트 의견 ID를 `910001~910500` 고정 범위로 생성하도록 변경했다.

```text
users: 100000~101199
rooms: 900001~900010
speeches: 910001~910500
```

따라서 20장의 결과를 기준 결과로 사용하고, 19장은 혼합 부하 병목 탐색 과정의 참고 기록으로만 본다.

### 20.1 재측정 1차: HTTP 목표 175 RPS + WebSocket 500명

```bash
READ_RATE=100 \
WRITE_RATE=25 \
STAGE_RATE=50 \
WS_VUS=500 \
MESSAGE_INTERVAL_SECONDS=5 \
DURATION=30s \
SPEECH_ID_BASE=910001 \
SPEECH_COUNT=500 \
k6 run performance/k6/target-scale-mixed-limit.js
```

| 항목 | 결과 |
| --- | ---: |
| HTTP 요청 수 | 28,373 |
| HTTP 처리량 | 887.23 req/s |
| HTTP 실패율 | 0% |
| HTTP p95 | 436.56ms |
| HTTP p99 | 586.58ms |
| dropped iterations | 185 |
| WebSocket 연결 수 | 500 |
| WebSocket connect p95 | 619.04ms |
| WebSocket 메시지 전송 | 2,731 |
| WebSocket 메시지 수신 | 152 |
| WebSocket failure rate | 69.59% |

HTTP 요청은 5xx 없이 처리됐고 p95도 500ms 이내였지만, WebSocket 브로드캐스트 수신은 크게 밀렸다.

### 20.2 재측정 2차: HTTP 목표 350 RPS + WebSocket 500명

```bash
READ_RATE=200 \
WRITE_RATE=50 \
STAGE_RATE=100 \
WS_VUS=500 \
MESSAGE_INTERVAL_SECONDS=5 \
DURATION=30s \
SPEECH_ID_BASE=910001 \
SPEECH_COUNT=500 \
k6 run performance/k6/target-scale-mixed-limit.js
```

| 항목 | 결과 |
| --- | ---: |
| HTTP 요청 수 | 33,997 |
| HTTP 처리량 | 973.74 req/s |
| HTTP 실패율 | 0% |
| HTTP p95 | 1.64s |
| HTTP p99 | 1.84s |
| dropped iterations | 2,979 |
| WebSocket 연결 수 | 500 |
| WebSocket connect p95 | 526ms |
| WebSocket 메시지 전송 | 2,635 |
| WebSocket 메시지 수신 | 49 |
| WebSocket failure rate | 90.20% |

부하를 높이면 HTTP p95가 1초 이상으로 증가하고, k6 dropped iteration도 크게 증가했다.
WebSocket 연결 자체는 유지되지만 메시지 수신은 거의 되지 않았다.

### 20.3 DB 반영량 확인

재측정 후 DB 반영량을 확인했다.

| 데이터 | 결과 |
| --- | ---: |
| chat_messages | 5,366 |
| speech_reports | 475 |
| speech_reactions | 10,000 |
| speaking_queue | 120 |

write API가 단순히 404로 빠지는 테스트가 아니라 실제 DB 저장과 상태 변경을 발생시키는 부하였음을 확인했다.

### 20.4 Prometheus 지표

혼합 부하 시간대의 주요 지표는 다음과 같다.

| 지표 | 결과 |
| --- | ---: |
| Hikari active connections max | 10 |
| Hikari pending connections max | 191 |
| Hikari acquire max | 1.109s |
| Hikari usage max | 1.395s |
| MySQL threads running max | 5 |
| WebSocket inbound executor pool size | 1 |
| WebSocket outbound executor pool size | 1 |
| WebSocket inbound queued tasks max | 100,527 |
| WebSocket outbound queued tasks max | 100,527 |
| WebSocket heartbeat scheduler queued tasks max | 100,527 |

API별 평균 응답 시간은 대부분 180~203ms 수준으로 비슷하게 증가했다.
특정 단일 API 하나만 느려진 것이 아니라 DB 커넥션 풀 대기와 WebSocket executor 큐 적체가 같이 나타났다.

### 20.5 병목 원인

현재 로컬 혼합 부하의 1차 병목은 WebSocket 메시지 처리 executor 큐 적체다.

근거:

- WebSocket 단독 500명/5초 테스트는 failure rate 0%로 통과했다.
- 같은 WebSocket 조건에서 REST 조회/쓰기/발언권 신청을 동시에 실행하면 수신 실패율이 69~90%로 증가했다.
- Prometheus에서 `clientInboundChannelExecutor`, `clientOutboundChannelExecutor`, `webSocketHeartbeatTaskScheduler`의 queued task가 100,000 이상까지 증가했다.
- WebSocket executor pool size가 1로 관측됐다.
- HTTP 5xx는 없지만 Hikari pending connection이 191까지 증가해 DB 커넥션 대기도 동시에 발생했다.

즉, 병목은 단순히 DB 하나만의 문제가 아니라 다음 조합으로 보는 것이 맞다.

```text
HTTP 조회/쓰기/발언권 신청 증가
→ DB 커넥션 풀 대기 증가
→ 채팅 저장 및 이벤트 처리 지연
→ WebSocket inbound/outbound executor 큐 적체
→ STOMP 메시지 브로드캐스트 수신 지연/누락
```

### 20.6 개선 우선순위

| 우선순위 | 개선 후보 | 이유 |
| ---: | --- | --- |
| 1 | WebSocket inbound/outbound channel executor pool/queue 설정 | 현재 pool size 1, queue 100k 이상 적체 |
| 2 | WebSocket heartbeat scheduler pool size 조정 | heartbeat 작업도 큐 적체에 포함됨 |
| 3 | 채팅 저장 시간과 브로드캐스트 시간 메트릭 분리 | 저장 병목인지 발행 병목인지 더 세밀하게 분해 필요 |
| 4 | Hikari pool size와 DB query 비용 재측정 | pending 191로 DB 대기 발생 |
| 5 | 신뢰도/베스트 의견/목록 조회 캐시 또는 집계 최적화 검토 | 혼합 조회가 누적 DB 부하를 만든다 |
| 6 | 운영 환경에서 외부 broker relay 검토 | 단일 인스턴스 simple broker의 브로드캐스트 한계 대비 |

현재 단계에서 바로 확인된 가장 명확한 수정 후보는 WebSocket 채널 executor 설정이다.
단, executor pool만 키우면 DB 대기가 더 커질 수 있으므로 적용 후 같은 혼합 부하로 재측정해야 한다.

## 21. WebSocket executor 튜닝 후 재측정

### 21.1 적용한 변경

혼합 부하에서 WebSocket executor pool size가 1로 동작하고 큐가 100,000건 이상 적체되는 것이 확인되어 WebSocket 채널 executor 설정을 명시했다.

| 설정 | 변경 후 기본값 |
| --- | ---: |
| inbound core pool size | 4 |
| inbound max pool size | 16 |
| inbound queue capacity | 2,000 |
| outbound core pool size | 4 |
| outbound max pool size | 16 |
| outbound queue capacity | 2,000 |
| heartbeat scheduler pool size | 4 |

환경 변수로 조정 가능하도록 구성했다.

```text
WEBSOCKET_INBOUND_CORE_POOL_SIZE
WEBSOCKET_INBOUND_MAX_POOL_SIZE
WEBSOCKET_INBOUND_QUEUE_CAPACITY
WEBSOCKET_OUTBOUND_CORE_POOL_SIZE
WEBSOCKET_OUTBOUND_MAX_POOL_SIZE
WEBSOCKET_OUTBOUND_QUEUE_CAPACITY
WEBSOCKET_HEARTBEAT_POOL_SIZE
```

### 21.2 재측정 1차: HTTP 목표 175 RPS + WebSocket 500명

| 항목 | 튜닝 전 | 튜닝 후 |
| --- | ---: | ---: |
| HTTP 요청 수 | 28,373 | 28,280 |
| HTTP 처리량 | 887.23 req/s | 898.95 req/s |
| HTTP 실패율 | 0% | 0% |
| HTTP p95 | 436.56ms | 438.34ms |
| HTTP p99 | 586.58ms | 547.28ms |
| dropped iterations | 185 | 196 |
| WebSocket 연결 수 | 500 | 500 |
| WebSocket connect p95 | 619.04ms | 823.20ms |
| WebSocket 메시지 전송 | 2,731 | 2,642 |
| WebSocket 메시지 수신 | 152 | 9,248 |
| WebSocket failure rate | 69.59% | 0% |

낮은 혼합 부하에서는 HTTP 성능은 거의 유지되면서 WebSocket 수신 실패가 해소됐다.

### 21.3 재측정 2차: HTTP 목표 350 RPS + WebSocket 500명

| 항목 | 튜닝 전 | 튜닝 후 |
| --- | ---: | ---: |
| HTTP 요청 수 | 33,997 | 28,550 |
| HTTP 처리량 | 973.74 req/s | 801.45 req/s |
| HTTP 실패율 | 0% | 0% |
| HTTP p95 | 1.64s | 2.27s |
| HTTP p99 | 1.84s | 3.60s |
| dropped iterations | 2,979 | 3,693 |
| WebSocket 연결 수 | 500 | 500 |
| WebSocket connect p95 | 526ms | 547.04ms |
| WebSocket 메시지 전송 | 2,635 | 2,626 |
| WebSocket 메시지 수신 | 49 | 10,252 |
| WebSocket failure rate | 90.20% | 0% |

높은 혼합 부하에서도 WebSocket 수신 실패는 해소됐다.
다만 WebSocket 메시지가 실제로 처리되면서 애플리케이션과 DB가 수행하는 총 작업량이 증가했고, HTTP p95와 dropped iteration은 더 나빠졌다.

### 21.4 튜닝 후 Prometheus 지표

튜닝 후 혼합 부하 시간대 주요 지표는 다음과 같다.

| 지표 | 결과 |
| --- | ---: |
| 현재 앱 Hikari active connections max | 10 |
| 현재 앱 Hikari pending connections max | 194 |
| Hikari acquire max | 1.799s |
| Hikari usage max | 2.341s |
| WebSocket inbound executor active max | 4 |
| WebSocket outbound executor active max | 4 |
| heartbeat scheduler active max | 4 |
| WebSocket executor queued tasks max | 66,755 |

WebSocket executor 큐 적체는 100,527에서 66,755로 감소했고, 수신 실패율은 0%가 됐다.
반면 Hikari pending connection은 여전히 190 이상으로 관측되어, 다음 병목은 DB 커넥션 풀 대기와 트랜잭션 처리 비용으로 보는 것이 타당하다.

### 21.5 튜닝 후 DB 반영량

| 데이터 | 결과 |
| --- | ---: |
| chat_messages | 5,268 |
| speech_reports | 475 |
| speech_reactions | 10,000 |
| speaking_queue | 120 |

WebSocket 채팅과 REST 쓰기가 실제 DB 저장까지 수행되는 부하였음을 다시 확인했다.

### 21.6 결론

이번 튜닝으로 1차 병목이던 WebSocket executor pool size 1 문제는 완화됐다.

```text
튜닝 전:
WebSocket executor pool size 1
→ 큐 100,000건 이상 적체
→ WebSocket 수신 실패율 69~90%

튜닝 후:
WebSocket executor pool size 4 이상
→ WebSocket 수신 실패율 0%
→ 실제 메시지 처리량 증가
→ DB 커넥션 풀 대기 병목이 더 명확히 드러남
```

현재 남은 병목은 특정 API 하나가 아니라 혼합 부하에서 발생하는 공통 DB 대기다.
API별 평균 응답 시간이 대부분 비슷하게 증가했고, Hikari pending connection이 194까지 증가했다.

다음 개선 후보는 다음 순서가 적절하다.

| 우선순위 | 개선 후보 | 판단 근거 |
| ---: | --- | --- |
| 1 | Hikari pool size, DB max connection, 애플리케이션 thread 수를 함께 조정 | Hikari pending connection이 190 이상 발생 |
| 2 | 목록/집계 조회 최적화 | 의견 목록, 참여자 수, 신뢰도, 베스트 의견 조회가 혼합 조회 부하를 만든다 |
| 3 | 채팅 저장과 브로드캐스트 시간 메트릭 분리 | DB 저장 병목과 WebSocket 발행 병목을 더 세분화해야 함 |
| 4 | performance profile에서 불필요한 백그라운드 작업 최소화 | AI/스케줄러 로그가 테스트 중 함께 발생함 |
| 5 | 운영 환경에서 WebSocket broker relay 검토 | 단일 인스턴스 simple broker 한계 대비 |

현재 로컬 기준 목표 동시접속 500명 + HTTP 혼합 부하에서는 WebSocket 전달 자체보다 DB 커넥션 풀 대기가 다음 병목이다.
운영 서버 테스트에서는 서버 스펙, DB 스펙, Hikari pool, MySQL max connection을 함께 기록해야 동일한 수치를 해석할 수 있다.
