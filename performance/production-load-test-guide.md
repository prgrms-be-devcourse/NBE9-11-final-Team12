# 운영 서버 부하 테스트 실행 가이드

## 1. 목적

운영 서버와 유사한 환경에서 현재 서비스가 목표 동시접속 규모를 처리할 수 있는지 확인한다.

현재 기준 서버 구성은 다음 전제를 둔다.

| 항목 | 기준 |
| --- | --- |
| Backend + Redis | AWS EC2 `t3.medium` |
| MySQL | AWS EC2 `t3.small` |
| 목표 토론방 | 10개 |
| 방당 동시접속자 | 50~100명 |
| 총 동시접속자 | 500~1000명 |
| 목표 HTTP 처리량 | 약 500~600 RPS |
| 목표 WebSocket 연결 | 800~1000개 |

부하 테스트는 장애를 내기 위한 작업이 아니라, 목표 부하에서 실패율과 p95 응답 시간을 확인하고 병목 가능성을 기록하는 작업이다.

---

## 2. 비용과 안전 원칙

테스트 비용을 1만 원 이하로 유지하기 위해 다음 원칙을 지킨다.

- 테스트 전체 시간은 30~60분 이내로 제한한다.
- `spike` 단계는 최대 2분만 실행한다.
- `stress` 또는 무제한 한계 테스트는 이 PR 범위에서 하지 않는다.
- 테스트 중 Grafana Cloud와 EC2 지표를 계속 확인한다.
- 5xx, DB 커넥션 대기, CPU 급등이 보이면 즉시 중단한다.

T3 계열은 CPU credit 정책이 있으므로 장시간 높은 CPU를 유지하면 추가 비용이 발생할 수 있다.
따라서 운영 서버에서는 짧은 단계별 테스트만 수행한다.

---

## 3. 테스트 전 준비물

### 3.1 로컬 또는 실행 PC

```bash
k6 version
mysql --version
```

없으면 설치 후 진행한다.

### 3.2 운영 서버 모니터링

Grafana Cloud에서 다음 지표가 들어오는지 먼저 확인한다.

```promql
up
http_server_requests_seconds_count
hikaricp_connections_active
hikaricp_connections_pending
jvm_memory_used_bytes
process_cpu_usage
redis_up
mysql_up
```

LogQL 로그 조회도 가능해야 한다.

```logql
{job="sisibibi-api"}
```

### 3.3 DB 접속 정보

성능 테스트 데이터 준비/삭제 스크립트는 MySQL 접속 정보가 필요하다.

```bash
export DB_HOST=<mysql-host>
export DB_PORT=3306
export DB_NAME=sisibibi
export DB_USERNAME=<username>
export DB_PASSWORD=<password>
```

운영 DB에 실제 사용자 데이터가 있는 경우 반드시 팀 합의 후 실행한다.
스크립트는 `[PERF]` prefix와 고정 ID 범위만 사용하지만, DB 쓰기/삭제 작업이므로 주의한다.

---

## 4. 테스트 데이터 준비

성능 테스트는 다음 고정 데이터를 사용한다.

| 데이터 | 범위 |
| --- | --- |
| 테스트 사용자 | `100000~101199` |
| 테스트 토픽 | `900001~900010` |
| 테스트 토론방 | `900001~900010` |
| 테스트 의견 | `910001~910500` |
| 방당 참여자 | 100명 |

준비 스크립트는 먼저 기존 성능 테스트 데이터를 삭제한 뒤 다시 생성한다.

```bash
CONFIRM_PERFORMANCE_DATA_WRITE=YES \
DB_HOST=<mysql-host> \
DB_PORT=3306 \
DB_NAME=sisibibi \
DB_USERNAME=<username> \
DB_PASSWORD=<password> \
performance/scripts/prepare-load-data.sh
```

정상 완료 시 다음 범위를 k6에서 그대로 사용한다.

```bash
ROOM_IDS=900001,900002,900003,900004,900005,900006,900007,900008,900009,900010
USER_ID_BASE=100000
USERS_PER_ROOM=100
SPEECH_ID_BASE=910001
SPEECH_COUNT=500
```

---

## 5. 부하 단계

`performance/scripts/run-prod-load.sh` 하나로 단계별 부하를 실행한다.

공통 변수:

```bash
export BASE_URL=https://<운영-api-도메인>
export FRONTEND_ORIGIN=https://<프론트-도메인>
```

### 5.1 Smoke

목적은 서버 연결, 인증 쿠키 생성, 테스트 데이터, WebSocket 연결이 정상인지 확인하는 것이다.

```bash
BASE_URL=https://<운영-api-도메인> \
FRONTEND_ORIGIN=https://<프론트-도메인> \
performance/scripts/run-prod-load.sh smoke
```

기본 부하:

| 항목 | 값 |
| --- | ---: |
| 시간 | 3분 |
| 예상 HTTP RPS | 약 49 |
| WebSocket VU | 50 |

Smoke에서 401, 403, 404가 반복되면 부하를 올리지 않는다.

### 5.2 Baseline

```bash
BASE_URL=https://<운영-api-도메인> \
FRONTEND_ORIGIN=https://<프론트-도메인> \
performance/scripts/run-prod-load.sh baseline
```

| 항목 | 값 |
| --- | ---: |
| 시간 | 5분 |
| 예상 HTTP RPS | 약 155 |
| WebSocket VU | 200 |

### 5.3 Half Load

목표 부하의 절반 수준이다.

```bash
BASE_URL=https://<운영-api-도메인> \
FRONTEND_ORIGIN=https://<프론트-도메인> \
performance/scripts/run-prod-load.sh half
```

| 항목 | 값 |
| --- | ---: |
| 시간 | 7분 |
| 예상 HTTP RPS | 약 310 |
| WebSocket VU | 500 |

### 5.4 Target Load

목표 규모 검증 단계다.

```bash
BASE_URL=https://<운영-api-도메인> \
FRONTEND_ORIGIN=https://<프론트-도메인> \
performance/scripts/run-prod-load.sh target
```

| 항목 | 값 |
| --- | ---: |
| 시간 | 10분 |
| 예상 HTTP RPS | 약 575 |
| WebSocket VU | 800 |

### 5.5 Short Spike

짧은 순간 부하만 확인한다. 비용과 장애 위험 때문에 2분을 넘기지 않는다.

```bash
BASE_URL=https://<운영-api-도메인> \
FRONTEND_ORIGIN=https://<프론트-도메인> \
performance/scripts/run-prod-load.sh spike
```

| 항목 | 값 |
| --- | ---: |
| 시간 | 2분 |
| 예상 HTTP RPS | 약 795 |
| WebSocket VU | 1000 |

---

## 6. RPS 계산 기준

`target-scale-mixed-limit.js`는 혼합 시나리오다.

| 시나리오 | 1 iteration당 요청 |
| --- | --- |
| Read | HTTP GET 9개 |
| Write | HTTP 약 2~3개 |
| Stage | HTTP POST 1개 |
| WebSocket | 연결 유지 + 주기적 채팅 SEND |

스크립트는 다음 방식으로 예상 HTTP RPS를 계산한다.

```text
예상 HTTP RPS = READ_RATE × 9 + WRITE_RATE × 3 + STAGE_RATE
```

예를 들어 target 단계는 다음과 같다.

```text
READ_RATE=55
WRITE_RATE=20
STAGE_RATE=20

55 × 9 + 20 × 3 + 20 = 575 RPS
```

WebSocket은 HTTP RPS와 별도로 연결 수와 메시지 송수신량을 본다.

---

## 7. Grafana에서 볼 지표

발표 자료나 결과 정리에는 다음 패널을 우선 사용한다.

| 지표 | 보는 이유 |
| --- | --- |
| HTTP RPS | 실제 처리량 확인 |
| HTTP p95/p99 by URI | 느린 API 식별 |
| HTTP 4xx/5xx | 정책 거절과 서버 오류 구분 |
| HikariCP active/idle/pending | DB 커넥션 병목 확인 |
| MySQL Threads / Slow Query | DB 대기와 느린 쿼리 확인 |
| Redis ops/s | Redis 사용량 확인 |
| JVM Heap / GC Pause | 메모리 압박 확인 |
| Host CPU / Memory | EC2 자원 한계 확인 |
| WebSocket 연결/메시지 지표 | 실시간 채팅 부하 확인 |

4xx는 항상 장애가 아니다. 중복 공감, 중복 신고, 발언권 중복 신청 같은 정책 거절도 포함된다.
반면 5xx는 서버 오류이므로 0에 가까워야 한다.

---

## 8. 중단 기준

다음 중 하나라도 발생하면 테스트를 중단한다.

| 조건 | 기준 |
| --- | --- |
| HTTP 5xx | 1% 이상 |
| p95 | 3초 이상이 2분 이상 지속 |
| Hikari pending | 지속적으로 증가 |
| MySQL slow query | 초당 증가 또는 특정 쿼리 반복 |
| EC2 CPU | 85% 이상이 3분 이상 지속 |
| Memory | 지속 증가 후 회복 없음 |
| WebSocket 실패율 | 5% 이상 |

---

## 9. 테스트 데이터 삭제

테스트 종료 후 반드시 정리한다.

```bash
CONFIRM_PERFORMANCE_DATA_DELETE=YES \
DB_HOST=<mysql-host> \
DB_PORT=3306 \
DB_NAME=sisibibi \
DB_USERNAME=<username> \
DB_PASSWORD=<password> \
performance/scripts/cleanup-load-data.sh
```

삭제 대상은 성능 테스트 고정 ID 범위와 `[PERF]` prefix 데이터다.

---

## 10. 결과 기록

테스트 후 `performance/results-template.md`를 복사해 결과를 남긴다.

기록할 내용:

- 실행 일시
- 실행 단계
- BASE_URL
- 테스트 데이터 범위
- k6 요약 결과
- Grafana 캡처
- p95/p99가 높은 URI
- 4xx와 5xx 구분
- Hikari pending 여부
- MySQL slow query 여부
- Redis timeout 여부
- 개선 필요 사항

