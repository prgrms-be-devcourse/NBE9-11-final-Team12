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
