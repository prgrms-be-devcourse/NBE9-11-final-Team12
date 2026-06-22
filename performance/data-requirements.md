# Performance Test Data Requirements

## 목적

k6 스크립트가 사용할 성능 테스트 전용 데이터의 최소 조건을 정리한다.

스크립트 주석만 AI에 전달하면 대략적인 초기화 코드는 만들 수 있지만 다음 내용은 확정할 수 없다.

- 현재 Entity와 DB 제약 조건
- 필요한 사용자 수
- 사용할 ID 범위
- 작성자와 요청 사용자의 분리
- 테스트 후 데이터 정리 방법

따라서 초기화 코드를 요청할 때 이 문서와 실행할 k6 명령을 함께 전달한다.

## 공통 원칙

- 일반 `local` 프로필의 `LocalDataInitializer`와 분리한다.
- `performance` 전용 Profile 또는 명시적 활성화 설정에서만 실행한다.
- 운영 DB에서는 실행하지 않는다.
- 사용자, 토론방, 의견 ID 범위를 로그로 출력한다.
- 데이터 생성은 여러 번 실행해도 중복 적재되지 않게 한다.
- 테스트 종료 후 전용 데이터만 삭제하거나 테스트 DB 전체를 초기화할 수 있어야 한다.

## 필요 데이터 계산

### Constant VUs

`MODE=smoke`는 반복마다 다른 사용자 ID를 사용한다.

```text
필요 사용자 수 ≈ 전체 iteration 수
```

k6 결과의 `iterations`가 준비된 사용자 수보다 많으면 존재하지 않는 사용자 ID로 인증 실패한다.

### Constant Arrival Rate

```text
필요 사용자 수 ≈ rate × duration(초)
```

예시:

```text
WRITE_RATE=10
DURATION=5m

10 × 300초 = 쓰기 사용자 약 3,000명 필요
```

읽기와 쓰기 사용자는 상태 충돌을 피하기 위해 별도 범위를 사용하는 것이 안전하다.

## Core API Mixed

필수 데이터:

```text
OPEN 토론방: 1개 이상
테스트 사용자: 예상 iteration 수 이상
공감·신고 대상 의견: 10개 이상, Load에서는 500개 이상 권장
의견 작성자: 테스트 사용자 범위 밖의 사용자
사용자 token_version: TOKEN_VERSION과 동일
```

권장 ID 범위 예시:

```text
읽기 사용자: 100000~109999
쓰기 사용자: 200000~209999
의견 작성자: 90000~99999
```

Load 실행 시 `READ_USER_ID_BASE`, `WRITE_USER_ID_BASE`를 각각 위 범위의 시작값으로 전달한다.

## Auth Flow

필수 데이터:

```text
사용자: AUTH_USER_ID_BASE부터 AUTH_USER_COUNT명
이메일: perf-auth-{userId}@sisibibi.test
비밀번호: AUTH_PASSWORD와 동일
상태: ACTIVE
token_version: 제한 없음, 로그인 시 현재 값으로 토큰 발급
```

로그인만 측정:

```bash
MODE=login k6 run performance/k6/auth-flow.js
```

로그인 후 Refresh Token 재발급까지 측정:

```bash
MODE=flow k6 run performance/k6/auth-flow.js
```

## Stage Request

필수 데이터:

```text
OPEN 토론방: ROOM_ID 1개
테스트 사용자: RATE × DURATION 이상
사용자 token_version: TOKEN_VERSION과 동일
기존 WAITING 또는 ASSIGNED 신청: 없어야 함
```

## Room Join Capacity

필수 데이터:

```text
OPEN 토론방: ROOM_ID 1개
최대 인원: ROOM_CAPACITY와 동일
기존 JOINED 참여자: KNOWN_EXISTING_PARTICIPANTS와 동일
신규 사용자: USER_ID_BASE부터 ATTEMPTS명
사용자 token_version: TOKEN_VERSION과 동일
```

정원 100명, 현재 99명인 방에 신규 사용자 10명이 동시에 입장하는 예시:

```bash
ROOM_ID=1 \
ROOM_CAPACITY=100 \
KNOWN_EXISTING_PARTICIPANTS=99 \
ATTEMPTS=10 \
USER_ID_BASE=400000 \
k6 run performance/k6/room-join-capacity.js
```

성공 입장은 최대 1건이어야 하며 나머지는 정원 초과 응답이어야 한다.

## Stage Queue Current

필수 데이터:

```text
OPEN 토론방: ROOM_ID_BASE부터 ROOM_COUNT개
테스트 사용자: INITIAL_USERS 이상
사용자 token_version: TOKEN_VERSION과 동일
Redis 대기열: 테스트 시작 전 초기 상태 확인
```

## Chat WebSocket

필수 데이터:

```text
OPEN 토론방: ROOM_ID 1개
테스트 사용자: USER_ID_BASE부터 VUS명
방 참여 상태: 모든 테스트 사용자가 JOINED
사용자 token_version: TOKEN_VERSION과 동일
FRONTEND_ORIGIN: Backend CORS 설정과 동일
```

연결·구독·채팅 전송 예시:

```bash
BASE_URL=http://localhost:8080 \
FRONTEND_ORIGIN=http://localhost:3000 \
ROOM_ID=1 \
USER_ID_BASE=500000 \
VUS=50 \
DURATION=3m \
CONNECTION_DURATION_SECONDS=30 \
MESSAGE_INTERVAL_SECONDS=2 \
TOKEN_VERSION=0 \
k6 run performance/k6/chat-websocket.js
```

## Initializer 생성 요청 예시

```text
AGENTS.md 규칙에 따라 performance 전용 Spring Profile에서만 실행되는
성능 테스트 데이터 Initializer를 작성해줘.

실행할 명령:
MODE=load READ_RATE=30 WRITE_RATE=10 DURATION=5m

performance/data-requirements.md와 현재 Entity/Repository/DB 제약을 확인하고,
필요한 사용자·OPEN 토론방·의견 데이터를 배치로 생성해줘.

조건:
- 운영 Profile에서는 절대 실행되지 않음
- 반복 실행 시 중복 생성 방지
- 생성된 USER_ID_BASE, ROOM_ID, SPEECH_ID_BASE, SPEECH_COUNT 로그 출력
- 테스트 사용자와 의견 작성자 분리
- token_version은 0으로 생성
- 테스트와 정리 방법 포함
```
