# Performance Test Scenarios

## 1. 로컬 성능 테스트 시나리오

로컬 테스트는 기능별 병목을 빠르게 확인하는 목적이다.

### Smoke

목적:

- 서버 실행 상태 확인
- 인증 쿠키, CSRF, 기본 API 응답 확인
- 스크립트 오류 확인

권장 조건:

```text
VU: 1~5
duration: 30s~1m
```

확인 API:

- 내 정보 조회
- 진행 중인 토론방 조회
- 의견 목록 조회
- 현재 발언자 조회

### Baseline

목적:

- 현재 로컬 환경에서 정상 처리 가능한 기본 응답 시간 확인
- 기능별 평균 응답 시간 확인

권장 조건:

```text
VU: 10~30
duration: 3m~5m
```

확인 API:

- 인증 사용자 조회
- 토론방 상세 조회
- 토론방 입장
- 의견 목록 조회
- 의견 작성
- 공감 등록/취소
- 신고 등록
- 발언권 신청
- 내 발언권 상태 조회

### Load

목적:

- 예상 서비스 규모에 가까운 부하에서 실패율과 p95 확인

현재 규모 산정:

```text
토론방: 5~10개
토론방당 동시 접속자: 50~100명
총 동시 접속자: 500~1000명
```

로컬에서는 운영 규모 전체를 그대로 재현하지 않는다.

권장 조건:

```text
VU: 50~100
duration: 5m~10m
```

### Stress

목적:

- 어느 지점부터 실패율과 지연 시간이 급격히 증가하는지 확인

주의:

- 로컬 PC 자원 한계가 먼저 올 수 있다.
- 운영 서버에 바로 적용하지 않는다.

권장 조건:

```text
VU: 단계적 증가
duration: 짧게
```

## 2. 운영 서버 성능 테스트 주의사항

운영 서버 테스트는 실제 인프라 비용과 장애 위험이 있다.

운영 서버에서는 다음 순서만 허용한다.

```text
1. Smoke
2. Baseline
3. Load
4. Stress는 팀 합의 후 별도 진행
```

운영 서버 테스트 전 확인:

- 테스트 시간 공지
- 대상 서버 확인
- 테스트 계정 사용
- 결제 API 제외
- 관리자 API 제외 또는 별도 승인
- DB/Redis 모니터링 준비
- 로그 수집 확인

운영 서버 테스트 중단 기준:

- 5xx 급증
- DB connection pool 포화
- Redis timeout 발생
- CPU 또는 Memory 급등
- 실제 사용자 기능 장애 발생

## 3. 핵심 API 시나리오

### 인증

목적:

- 쿠키 기반 인증 검증 비용 확인
- `TokenSessionValidator`의 사용자 상태/tokenVersion 조회 영향 확인

대상:

- `GET /api/v1/users/me`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/reissue`

주의:

- 로그인은 PasswordEncoder 비용이 있으므로 일반 조회 API와 분리해서 본다.
- 재발급은 Redis와 DB 검증을 함께 사용한다.

### 토론방

목적:

- 토론방 조회와 입장 부하 확인
- 정원 검증 및 참여자 상태 변경 영향 확인

대상:

- `GET /api/v1/rooms/open`
- `GET /api/v1/rooms/{roomId}`
- `POST /api/v1/rooms/{roomId}/participants`
- `POST /api/v1/rooms/{roomId}/participants/out`
- `GET /api/v1/rooms/{roomId}/participants/count`

### 의견

목적:

- 의견 목록 페이징 조회 성능 확인
- 공감 정보 집계 포함 응답 비용 확인
- 욕설 필터링 포함 작성 비용 확인

대상:

- `GET /api/v1/rooms/{roomId}/speeches`
- `POST /api/v1/rooms/{roomId}/speeches`
- `GET /api/v1/speeches/{speechId}`

### 공감

목적:

- 중복 공감 방어와 공감 수 집계 영향 확인
- WebSocket 이벤트 발행 포함 비용 확인

대상:

- `POST /api/v1/speeches/{speechId}/reactions`
- `DELETE /api/v1/speeches/{speechId}/reactions`
- `GET /api/v1/rooms/{roomId}/best-speech`

### 신고

목적:

- 중복 신고 방어와 신고 저장 비용 확인
- 신고 상세 설명 검증 비용 확인

대상:

- `POST /api/v1/speeches/{speechId}/reports`

주의:

- 같은 사용자가 같은 의견을 반복 신고하면 중복 신고로 실패한다.
- 성능 테스트에서는 사용자 ID와 신고 대상 의견 ID를 분산한다.

### 발언권

목적:

- 발언권 신청 동시성 확인
- RDB 원본 저장과 Redis projection 동기화 비용 확인
- 현재 발언자/내 순번 조회 비용 확인

대상:

- `POST /api/v1/rooms/{roomId}/stage/requests`
- `GET /api/v1/rooms/{roomId}/stage/requests/me`
- `GET /api/v1/rooms/{roomId}/stage`
- `POST /api/v1/rooms/{roomId}/stage/complete`

## 4. 부하 단계별 기준

| 단계 | 목적 | 권장 부하 | 판단 기준 |
| --- | --- | --- | --- |
| Smoke | 실행 확인 | 1~5 VU | 요청 성공 여부 |
| Baseline | 기준 성능 측정 | 10~30 VU | p95, 실패율 |
| Load | 예상 부하 확인 | 50~100 VU | p95 2초 미만, 실패율 5% 미만 |
| Stress | 한계 확인 | 단계 증가 | 병목 지점 확인 |

## 5. 결과 해석 기준

성능 저하 원인을 API 코드만으로 단정하지 않는다.

함께 확인할 것:

- 애플리케이션 로그
- DB slow query
- DB connection pool
- Redis timeout
- JVM memory
- CPU 사용률
- 네트워크 지연
- 프론트 요청 중복 여부

## 6. WebSocket 채팅

목적:

- WebSocket handshake 성공률 확인
- STOMP CONNECT와 SUBSCRIBE 처리 시간 확인
- 동시 연결 유지 중 채팅 SEND와 브로드캐스트 확인
- 채팅 Rate Limiter와 메시지 저장 부하 확인

대상:

- `CONNECT /api/v1/ws`
- `SUBSCRIBE /topic/rooms/{roomId}/chat/events`
- `SEND /app/rooms/{roomId}/chat/messages`

HTTP API 테스트와 분리해 실행한다. 연결 수, 메시지 전송 주기, 테스트 시간을 각각 단계적으로 증가시킨다.

## 7. 현재 범위에서 제외

이번 성능 테스트 시나리오 작성 범위에서 제외한다.

- 결제 API 부하 테스트
- 실제 운영 서버 고부하 테스트 실행
- 운영 서버 WebSocket 한계·장시간 soak 테스트
- Prometheus/Grafana 도입
- 성능 병목 리팩토링
- CI 성능 테스트 gate 적용

## 8. 목표 동시접속자 기준 테스트

현재 목표 규모는 10개 토론방, 방당 50~100명, 총 500~1000명이다.

이를 성능 테스트에서는 다음으로 나눠 본다.

```text
동시접속자 = WebSocket 연결 수와 화면 유지 사용자 수
RPS = 화면 조회/상태 확인 요청 수
TPS = 상태 변경 요청 수
```

권장 1차 목표:

| 항목 | 목표 |
| --- | --- |
| HTTP 읽기 RPS | 100~300 RPS |
| 발언권 신청 TPS | 10~50 TPS |
| 채팅 메시지 TPS | 10~30 TPS |
| WebSocket 연결 | 500~1000 connections |
| 실패율 | 1% 미만 |
| HTTP p95 | 1초 미만 |
| HTTP p99 | 2초 미만 |

테스트 중 병목 판단 순서:

```text
1. k6 실패율과 p95/p99 확인
2. Grafana HTTP p95/p99 by URI에서 느린 API 확인
3. HikariCP pending이 튀면 DB 커넥션 풀 병목 의심
4. MySQL threads/slow query가 튀면 쿼리 또는 인덱스 병목 의심
5. Redis timeout/ops 급증 확인
6. JVM heap/GC pause 확인
7. Host CPU/Memory 확인
8. Application logs에서 예외와 timeout 확인
```

## 9. 목표 규모 WebSocket 시나리오

### 목적

10개 토론방에 방당 50~100명의 사용자가 접속했을 때 WebSocket 연결, STOMP 구독, 채팅 브로드캐스트가 유지되는지 확인한다.

### 기준

| 구분 | 기준 |
| --- | --- |
| 방 수 | 10개 |
| 방당 동시 접속자 | 50~100명 |
| 총 동시 접속자 | 500~1000명 |
| 채팅 발생량 | 500명 기준 약 50 TPS, 1000명 기준 약 100 TPS |
| 실패율 | 5% 미만 |
| STOMP 연결 p95 | 2초 미만 |

### 병목 판단

- WebSocket 연결 실패가 증가하면 서버 thread, WebSocket handshake, 인증 필터를 확인한다.
- 메시지 수신 실패가 증가하면 STOMP broker, 브로드캐스트 경로, 채팅 저장 로직을 확인한다.
- HTTP API p95가 함께 증가하면 DB connection pool 또는 MySQL 병목 가능성이 높다.
- Redis ops/sec가 급증하고 응답이 느려지면 발언권/인증/제재 캐시 경로를 분리해서 확인한다.

## 목표 기준 산정 근거

현재 목표 규모는 다음과 같다.

```text
토론방 수: 10개
방당 동시 접속자: 50~100명
총 동시 접속자: 500~1000명
```

동시 접속자 수가 그대로 RPS가 되는 것은 아니다. 사용자가 몇 초마다 어떤 행동을 하는지 기준으로 RPS/TPS를 환산한다.

| 사용자 행동 | 산정 방식 | 목표값 |
| --- | --- | ---: |
| 화면 상태 조회 | 1000명이 10~30초마다 조회 | 30~100 RPS |
| 의견 목록·상세 조회 | 1000명이 10~20초마다 갱신 | 50~100 RPS |
| 보조 조회 | 내 정보, 제재, 신뢰도 등을 30~60초마다 조회 | 10~30 RPS |
| HTTP 읽기 혼합 | 여러 조회 API가 섞이는 상황 | 100~300 RPS |
| 채팅 | 방당 1~3 msg/s | 10~30 TPS |
| 발언권 신청 | 이벤트성 burst 요청 | 10~50 TPS |
| WebSocket | 접속자 수와 동일 | 500~1000 연결 |

따라서 `20 TPS` 발언권 신청은 기준 부하(load), `30~50 TPS`는 순간 집중을 보는 stress로 본다.

## 동시성 테스트 확장 기준

성능 테스트는 처리량과 응답 시간을 보고, 동시성 테스트는 상태 불변식이 깨지지 않는지 본다.

우선 검증 대상은 다음이다.

| 영역 | 검증 불변식 |
| --- | --- |
| 토론방 입장 | 정원 초과 입장 불가 |
| 발언권 신청 | 중복 신청 불가, 순번 중복 불가 |
| 공감 | 한 사용자는 한 의견에 1회만 공감 가능 |
| 의견 신고 | 한 사용자는 한 의견을 1회만 신고 가능 |
| 신고 처리 | 종결된 신고는 다시 처리 불가 |
| 사용자 제재 | 동일 유형 활성 제재 중복 방지 |
| WebSocket presence | 접속/해제 순서가 꼬여도 최종 상태 복구 |

현재 k6로 검증한 동시성 시나리오는 다음이다.

| 스크립트 | 목적 |
| --- | --- |
| `room-join-capacity.js` | 정원 초과 동시 입장 방어 |
| `target-scale-stage.js` | 발언권 신청 TPS와 순번 발급 경합 |
| `reaction-race.js` | 동일 사용자 동일 의견 공감 중복 생성 방어 |
| `speech-report-race.js` | 동일 사용자 동일 의견 신고 중복 생성 방어 |
