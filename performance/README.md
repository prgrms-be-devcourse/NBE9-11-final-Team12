# Performance Test Guide

## 목적

현재 구현된 MVP와 추가 기능을 대상으로 로컬/운영 서버 성능 테스트 기준을 맞추기 위한 문서다.

성능 테스트는 병목을 단정하기 위한 작업이 아니라, 다음 항목을 확인하기 위한 작업이다.

- 핵심 API의 응답 시간과 실패율
- 인증/인가 검증이 포함된 실제 요청 흐름
- 토론방 입장, 의견, 공감, 신고, 발언권 요청의 부하 특성
- 운영 서버 부하 테스트 전 안전 기준
- 테스트 결과를 팀이 같은 형식으로 기록하기 위한 기준

## 디렉터리 구조

```text
performance/
├── README.md
├── scenarios.md
├── results-template.md
└── k6/
    ├── core-api-mixed.js
    ├── auth-flow.js
    ├── chat-websocket.js
    ├── room-join-capacity.js
    ├── stage-request.js
    ├── stage-queue-current.js
    └── lib/
        └── auth.js
```

## 로컬 실행 전 준비

로컬 성능 테스트는 운영 서버 부하 테스트 전에 수행한다.

필수 준비:

- Backend 실행
- MySQL 실행
- Redis 실행
- 테스트용 사용자, 토론방, 의견 데이터 준비
- k6 설치

```bash
k6 version
```

기본 실행 예시:

```bash
BASE_URL=http://localhost:8080 \
ROOM_ID=1 \
SPEECH_ID_BASE=1 \
SPEECH_COUNT=10 \
USER_ID_BASE=100000 \
k6 run performance/k6/core-api-mixed.js
```

## 실행 전 데이터 조건

`core-api-mixed.js`는 JWT만 임의 생성하는 스크립트다. 사용자·토론방·의견을 자동 생성하지 않으므로 다음 데이터는 DB에 실제로 존재해야 한다.

| 항목 | 조건 |
| --- | --- |
| 사용자 | `USER_ID_BASE`부터 테스트 중 사용할 수만큼 존재 |
| Token Version | 테스트 사용자의 `users.token_version`과 `TOKEN_VERSION`이 일치 |
| 토론방 | `ROOM_ID`에 해당하는 OPEN 상태 방 존재 |
| 참여 상태 | 쓰기 시나리오 사용자는 입장 가능하거나 이미 참여 중 |
| 의견 | `SPEECH_ID_BASE`부터 `SPEECH_COUNT` 범위의 조회 가능한 의견 존재 |
| 작성자 분산 | 공감·신고 대상 의견의 작성자와 테스트 사용자가 달라야 함 |

`USER_ID_BASE=100000`으로 실행한다고 사용자 ID가 자동 생성되지는 않는다.

현재 `LocalDataInitializer`는 화면과 기능 확인을 위한 소량 데이터만 생성한다. 반복마다 다른 사용자 ID를 사용하는 현재 k6 시나리오의 데이터로는 부족하다.

대량 테스트 전에는 다음 순서로 성능 테스트 전용 데이터를 준비한다.

```text
1. 성능 테스트 전용 초기화 코드 또는 SQL로 사용자·방·의견 적재
2. 적재된 실제 ID 범위 확인
3. k6 명령의 USER_ID_BASE, ROOM_ID, SPEECH_ID_BASE, SPEECH_COUNT 변경
4. 테스트 후 전용 데이터 정리 또는 테스트 DB 초기화
```

성능 테스트용 대량 초기화는 일반 `local` 프로필 시작마다 자동 실행하지 않는다. 평소 개발 실행에도 불필요한 데이터가 계속 쌓일 수 있기 때문이다.

성능 테스트 전용 Initializer를 추가할 경우에는 `performance` 같은 별도 Spring Profile 또는 명시적인 활성화 설정에서만 실행되게 구성한다.

```text
Smoke 예시
- 사용자 최소 100명
- OPEN 방 1개
- 의견 최소 10개

Baseline 예시
- 사용자 최소 5,000명
- OPEN 방 1~5개
- 의견 최소 100개

Load 예시
- 예상 iteration 수보다 많은 사용자
- 여러 OPEN 방
- 공감·신고 대상 의견 500개 이상
```

정확한 필요 사용자 수는 실행률과 시간에 따라 달라진다. `MODE=load`에서는 대략 `가장 높은 rate × duration(초)` 이상의 사용자 범위를 준비한다.

## 환경 변수 사용 원칙

스크립트 소스는 단계마다 수정하지 않는다. 실행 명령 앞의 환경 변수만 테스트 데이터와 부하 단계에 맞게 변경한다.

| 구분 | 변수 | 변경 기준 |
| --- | --- | --- |
| 환경 | `BASE_URL` | 로컬과 운영 대상이 바뀔 때 변경 |
| 인증 | `JWT_SECRET` | Backend 설정과 다를 때 변경 |
| 인증 | `TOKEN_VERSION` | 테스트 사용자 DB 값과 다를 때 변경 |
| 인증 | `CSRF_TOKEN` | 현재 구조에서는 기본값 유지 가능 |
| 데이터 | `ROOM_ID`, `ROOM_ID_BASE`, `ROOM_COUNT` | 적재한 토론방 ID에 맞게 항상 확인 |
| 데이터 | `SPEECH_ID_BASE`, `SPEECH_COUNT` | 적재한 의견 ID 범위에 맞게 항상 확인 |
| 데이터 | `USER_ID_BASE`, `INITIAL_USERS` | 적재한 사용자 ID 범위에 맞게 항상 확인 |
| 데이터 | `READ_USER_ID_BASE`, `WRITE_USER_ID_BASE` | Load 읽기·쓰기 사용자 범위를 분리 |
| 부하 | `VUS`, `RATE`, `READ_RATE`, `WRITE_RATE` | Smoke→Baseline→Load 순서로 증가 |
| 실행 | `DURATION` | 짧은 검증 후 점진적으로 증가 |

`BASE_URL`, `JWT_SECRET`, `TOKEN_VERSION`, `CSRF_TOKEN`은 같은 환경과 같은 테스트 계정을 사용한다면 매번 바꿀 필요가 없다.

반면 ID 변수는 테스트 DB 데이터에 종속되므로 실행 전 반드시 확인해야 한다.

### 스크립트마다 `USER_ID_BASE`가 다른 이유

각 스크립트의 기본 사용자 범위는 서로 다른 상태 변경이 섞이지 않도록 의도적으로 분리했다.

| 스크립트 | 기본 범위 | 분리 이유 |
| --- | --- | --- |
| `core-api-mixed.js` | `100000`부터 | 입장·의견·공감·신고·발언권 상태를 함께 생성 |
| `auth-flow.js` | `300000`부터 | 로그인·재발급용 계정과 상태 변경 API 사용자 분리 |
| `stage-request.js` | `1000000`부터 | 발언권 신청 집중 부하만 측정 |
| `stage-queue-current.js` | `930000000`부터 | 신청·순번 조회·발언 종료가 이어지는 상태 사용 |

동일 사용자를 여러 스크립트에서 재사용하면 중복 신고, 중복 공감, 이미 존재하는 발언권 신청 때문에 성능이 아니라 비즈니스 예외 비율을 측정하게 된다.

기본 ID를 반드시 유지할 필요는 없다. 실제 적재한 사용자 범위에 맞춰 실행 명령의 `USER_ID_BASE`를 변경한다.

## 단계별 실행 방법

### 1. 스크립트 확인

서버에 부하를 주지 않고 k6 문법과 옵션을 확인한다.

```bash
k6 inspect performance/k6/core-api-mixed.js
```

### 2. Smoke

목적은 성능 측정이 아니라 환경 변수, 인증, 테스트 데이터가 올바른지 확인하는 것이다.

```bash
MODE=smoke \
VUS=3 \
DURATION=30s \
BASE_URL=http://localhost:8080 \
ROOM_ID=1 \
SPEECH_ID_BASE=1 \
SPEECH_COUNT=10 \
USER_ID_BASE=100000 \
TOKEN_VERSION=0 \
k6 run performance/k6/core-api-mixed.js
```

Smoke에서 401, 403, 404가 반복되면 VU를 올리지 않고 다음을 먼저 확인한다.

- 401: JWT secret, 사용자 존재 여부, Token Version
- 403: CSRF, 방 참여 여부, 본인 의견 공감·신고 여부
- 404: 토론방·의견 ID
- 409: 동일 사용자 중복 참여·공감·신고·발언권 신청

### 3. Baseline

정상 상태의 기준 응답 시간을 측정한다.

```bash
MODE=smoke \
VUS=20 \
DURATION=3m \
BASE_URL=http://localhost:8080 \
ROOM_ID=1 \
SPEECH_ID_BASE=1 \
SPEECH_COUNT=100 \
USER_ID_BASE=100000 \
TOKEN_VERSION=0 \
k6 run performance/k6/core-api-mixed.js
```

Baseline에서는 VU와 시간만 증가한다. 사용자와 의견 데이터도 실행량에 맞게 충분히 준비해야 한다.

### 4. Load

`MODE=load`는 읽기와 쓰기를 초당 도착률 기준으로 분리한다.

```bash
MODE=load \
DURATION=5m \
READ_RATE=30 \
WRITE_RATE=10 \
READ_USER_ID_BASE=100000 \
WRITE_USER_ID_BASE=200000 \
READ_PRE_ALLOCATED_VUS=50 \
READ_MAX_VUS=200 \
WRITE_PRE_ALLOCATED_VUS=30 \
WRITE_MAX_VUS=120 \
BASE_URL=http://localhost:8080 \
ROOM_ID=1 \
SPEECH_ID_BASE=1 \
SPEECH_COUNT=500 \
USER_ID_BASE=100000 \
TOKEN_VERSION=0 \
k6 run performance/k6/core-api-mixed.js
```

| 값 | 의미 | 조정 방법 |
| --- | --- | --- |
| `READ_RATE` | 초당 읽기 시나리오 반복 수 | 10 → 30 → 50 순으로 증가 |
| `WRITE_RATE` | 초당 쓰기 시나리오 반복 수 | 2 → 5 → 10 순으로 증가 |
| `*_PRE_ALLOCATED_VUS` | k6가 미리 확보할 VU | 예상 응답 지연을 감당할 만큼 설정 |
| `*_MAX_VUS` | k6가 확장할 최대 VU | 무제한 증가 방지를 위해 상한 설정 |
| `DURATION` | 부하 유지 시간 | 1m 검증 후 5m, 이후 10m |

`WRITE_RATE=10`은 API 요청 10개라는 의미가 아니다. 쓰기 시나리오 한 번에 입장, 의견 작성, 공감 등록·취소, 신고, 발언권 신청을 순차 호출하므로 실제 HTTP 요청 수는 더 많다.

읽기와 쓰기는 서로 다른 사용자 범위를 사용한다. 두 범위를 겹치게 설정하면 쓰기 상태가 읽기 시나리오와 충돌할 수 있다.

## 인증 전용 스크립트

로그인만 측정:

```bash
MODE=login \
VUS=5 \
DURATION=1m \
BASE_URL=http://localhost:8080 \
AUTH_USER_ID_BASE=300000 \
AUTH_USER_COUNT=100 \
AUTH_EMAIL_PREFIX=perf-auth- \
AUTH_PASSWORD='test1234!' \
k6 run performance/k6/auth-flow.js
```

로그인 후 Refresh Token 재발급까지 측정:

```bash
MODE=flow \
VUS=5 \
DURATION=1m \
BASE_URL=http://localhost:8080 \
AUTH_USER_ID_BASE=300000 \
AUTH_USER_COUNT=100 \
AUTH_EMAIL_PREFIX=perf-auth- \
AUTH_PASSWORD='test1234!' \
k6 run performance/k6/auth-flow.js
```

로그인은 BCrypt와 DB 조회, 재발급은 Redis 회전과 사용자 검증 비용을 포함하므로 일반 API 혼합 부하와 분리해서 측정한다.

## 토론방 정원 동시성 스크립트

정원 직전 상태에서 여러 사용자가 동시에 입장할 때 최대 인원이 초과되지 않는지 확인한다.

```bash
BASE_URL=http://localhost:8080 \
ROOM_ID=1 \
ROOM_CAPACITY=100 \
KNOWN_EXISTING_PARTICIPANTS=99 \
ATTEMPTS=10 \
USER_ID_BASE=400000 \
TOKEN_VERSION=0 \
k6 run performance/k6/room-join-capacity.js
```

이 테스트는 응답 시간보다 정원 불변식 유지가 핵심이다. 실행 전 기존 참여자 수를 정확히 맞춰야 한다.

## WebSocket 채팅 스크립트

STOMP 연결, 방 이벤트 구독, 채팅 전송과 브로드캐스트 수신을 함께 측정한다.

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

모든 사용자는 해당 방에 `JOINED` 상태여야 한다. `MESSAGE_INTERVAL_SECONDS`를 서비스 채팅 제한보다 짧게 설정하면 WebSocket 처리량이 아니라 Rate Limiter 거절을 측정하게 된다.

`CONNECTION_DURATION_SECONDS`는 한 연결을 유지하는 시간이다. 전체 `DURATION`보다 짧게 설정하면 연결 종료 후 다음 iteration에서 재연결하며, 길게 설정하면 전체 실행 동안 연결 유지 성능을 본다.

### 5. Stress

Stress는 기본 스크립트 값을 한 번에 크게 올리지 않는다. Load 결과가 안정적인 경우 별도 승인 후 다음 순서로 진행한다.

```text
READ_RATE: 30 → 50 → 100
WRITE_RATE: 10 → 20 → 30
DURATION: 각 단계 2~3분
```

각 단계 사이에 DB 커넥션, Redis 오류, JVM 메모리, CPU가 정상으로 돌아왔는지 확인한다.

## 전용 발언권 스크립트

### 발언권 신청 집중 부하

```bash
BASE_URL=http://localhost:8080 \
ROOM_ID=910001 \
USER_ID_BASE=1000000 \
RATE=50 \
DURATION=20s \
PRE_ALLOCATED_VUS=20 \
MAX_VUS=100 \
TOKEN_VERSION=0 \
k6 run performance/k6/stage-request.js
```

### 발언권 신청·순번 조회·종료 혼합

```bash
MODE=realistic \
BASE_URL=http://localhost:8080 \
ROOM_ID_BASE=930000 \
ROOM_COUNT=10 \
USER_ID_BASE=930000000 \
INITIAL_USERS=1000 \
TOKEN_VERSION=0 \
k6 run performance/k6/stage-queue-current.js
```

발언권 전용 스크립트는 대량의 토론방·사용자 데이터가 준비되어 있다는 전제다. 기본 ID를 그대로 사용하기 전에 DB 적재 범위를 반드시 확인한다.

## 인증 방식

성능 테스트 스크립트는 기본적으로 테스트용 JWT를 생성해서 쿠키에 넣는다.

```text
accessToken={JWT}
XSRF-TOKEN=perf-csrf-token
```

주의:

- 테스트용 JWT의 `userId`는 DB에 존재해야 한다.
- 테스트용 JWT의 `tokenVersion`은 DB의 `users.token_version`과 같아야 한다.
- 기본값은 `TOKEN_VERSION=0`이다.

계정 정지/토큰 무효화 기능 이후에는 사용자 존재 여부와 `tokenVersion`이 검증되므로, 단순히 서명만 맞는 JWT로는 요청이 통과하지 않는다.

## 운영 서버 테스트 주의사항

운영 서버 성능 테스트는 반드시 팀 합의 후 낮은 부하부터 진행한다.

운영 서버에서 금지:

- 무제한 VU 실행
- 최대 부하부터 시작
- 실제 결제 요청
- 실제 사용자 계정 사용
- 모니터링 없이 장시간 실행

운영 서버 권장 순서:

```text
1. smoke: 1~5 VU, 1분 이하
2. baseline: 예상 트래픽의 10~20%
3. load: 예상 트래픽 수준
4. stress: 장애 지점 확인용, 별도 승인 필요
```

운영 서버에서는 비용과 장애 영향이 발생할 수 있으므로, 실행 시간과 VU 수를 명확히 제한한다.

운영 서버에서는 로컬 명령의 다음 값만 운영 테스트용으로 교체한다.

```text
BASE_URL=https://테스트-대상-도메인
JWT_SECRET=운영 서버와 동일한 테스트용 서명 설정
ROOM_ID=운영 성능 테스트 전용 토론방
SPEECH_ID_BASE/SPEECH_COUNT=운영 성능 테스트 전용 의견 범위
USER_ID_BASE=운영 성능 테스트 전용 사용자 범위
```

운영 Secret을 쉘 히스토리, 스크립트, Git에 저장하지 않는다. 가능하면 운영과 분리한 스테이징 환경에서 먼저 실행한다.

## 로컬 모니터링 실행

현재 Backend에는 Actuator와 Prometheus Registry가 이미 적용되어 있으며 `/actuator/prometheus`를 노출한다.

메트릭은 Prometheus, 로그는 Loki와 Promtail, 조회 화면은 Grafana가 담당한다.

```text
Backend
├─ /actuator/prometheus → Prometheus → Grafana 메트릭 패널
└─ backend/logs/*.log → Promtail → Loki → Grafana 로그 패널
```

Prometheus와 Grafana만으로는 애플리케이션 로그를 수집할 수 없으므로 Loki와 Promtail을 함께 구성한다.

### 1. Docker 서비스 실행

프로젝트 루트에서 실행한다.

```bash
mkdir -p backend/logs
cp monitoring/.env.example monitoring/.env
# monitoring/.env의 GRAFANA_ADMIN_PASSWORD를 로컬에서 사용할 값으로 변경
docker compose up -d mysql redis
docker compose --env-file monitoring/.env -f monitoring/docker-compose.monitoring.yml up -d
```

`backend/logs`를 먼저 생성하는 이유는 Docker가 없는 경로를 대신 만들면서 소유권이 달라지는 문제를 방지하기 위해서다. 디렉터리가 없으면 모니터링 Compose는 자동 생성하지 않고 명확히 실패한다.

Grafana 관리자 비밀번호는 저장소에 기본값을 두지 않는다. `monitoring/.env`는 Git에 포함하지 않으며, 로컬 실행 시 `monitoring/.env.example`을 복사해 설정한다.

접속 주소:

| 대상 | 주소 | 기본 인증 |
| --- | --- | --- |
| Grafana | `http://localhost:3001` | `monitoring/.env` 설정값 |
| Prometheus | `http://localhost:9091` | 없음 |

Grafana에는 Prometheus와 Loki datasource, `Sisibibi Local Overview` 대시보드가 자동 등록된다.

호스트 포트는 프로젝트 로컬 기본값이며 컨테이너 내부 포트와 별개다. 충돌하면 실행 시 변경할 수 있다.

```bash
GRAFANA_PORT=33001 \
PROMETHEUS_PORT=39091 \
docker compose --env-file monitoring/.env -f monitoring/docker-compose.monitoring.yml up -d
```

### 2. Backend 실행

IDE 실행 설정의 Active profiles에 다음 값을 입력한다.

```text
local,monitoring
```

터미널에서는 다음과 같이 실행한다.

```bash
cd backend
SPRING_PROFILES_ACTIVE=local,monitoring ./gradlew bootRun
```

`monitoring` 프로필은 `backend/logs/sisibibi-api.log`에 로그를 기록한다. 해당 디렉터리는 Git에 포함하지 않는다.

### 3. 수집 상태 확인

1. Backend의 `http://localhost:8080/actuator/prometheus`에서 메트릭이 반환되는지 확인
2. Prometheus `Status → Targets`에서 `sisibibi-api`가 `UP`인지 확인
3. Grafana `Dashboards → Sisibibi → Sisibibi Local Overview` 확인
4. API를 호출한 뒤 HTTP, JVM, CPU, 로그 패널이 갱신되는지 확인

Prometheus 컨테이너는 로컬 IDE에서 실행한 Backend를 `host.docker.internal:8080`으로 수집한다.

### 4. 종료와 초기화

컨테이너만 종료:

```bash
docker compose --env-file monitoring/.env -f monitoring/docker-compose.monitoring.yml stop
```

컨테이너와 모니터링 볼륨 삭제:

```bash
docker compose --env-file monitoring/.env -f monitoring/docker-compose.monitoring.yml down -v
```

일반 개발용 MySQL과 Redis는 루트 `docker-compose.yml`, 관측 도구는 `monitoring/docker-compose.monitoring.yml`로 분리한다. 따라서 평소 `docker compose up`으로 모니터링 서비스가 의도치 않게 함께 실행되지 않는다.

## 운영 배포 시 분리할 설정

로컬에서 검증한 대시보드와 기본 수집 구조는 운영에서도 재사용할 수 있다. 다만 로컬 Compose를 설정 변경 없이 그대로 실행하지 않는다.

| 공통 사용 | 운영에서 분리 |
| --- | --- |
| Prometheus/Grafana/Loki 이미지 버전 | Backend 대상 주소 |
| Grafana 대시보드 JSON | 데이터 보존 기간과 볼륨 |
| datasource provisioning 형식 | 관리자 비밀번호와 Secret |
| 공통 메트릭·로그 라벨 | 포트 공개와 네트워크 |
| 기본 scrape job | CPU/Memory 제한 |

Backend, Prometheus, Grafana, Loki를 하나의 이미지로 빌드하지 않는다. 각각 별도 공식 이미지를 사용하고 운영용 Compose 또는 배포 환경에서 연결한다.

운영에서는 Grafana와 Prometheus 포트를 인터넷에 직접 공개하지 않고, 사설 네트워크·방화벽·리버스 프록시 인증 등으로 접근을 제한해야 한다.

## 기본 판단 기준

초기 기준은 다음과 같이 둔다.

| 항목 | 기준 |
| --- | --- |
| HTTP 실패율 | 5% 미만 |
| p95 응답 시간 | 2초 미만 |
| p99 응답 시간 | 5초 미만 |
| 5xx 비율 | 1% 미만 |
| DB CPU | 급격한 포화 여부 확인 |
| Redis 오류 | 연결 실패/timeout 확인 |

이 수치는 최종 SLA가 아니라 초기 병목 탐지 기준이다.

## 테스트 결과 기록

테스트 결과는 `performance/results-template.md` 형식을 복사해서 기록한다.

결과 기록 시 반드시 포함할 것:

- 테스트 일시
- 대상 환경
- 실행 명령
- 테스트 데이터 ID 범위
- 주요 지표
- 실패율
- 병목 추정
- 후속 작업
