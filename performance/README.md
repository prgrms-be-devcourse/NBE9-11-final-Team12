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
    ├── stage-request.js
    ├── stage-queue-current.js
    └── lib/
        └── auth.js
```

## perf 디렉터리 사용 여부

다른 팀처럼 `perf/`를 둘 수도 있지만, 현재 저장소에는 이미 `performance/` 디렉터리가 있다.

따라서 새 디렉터리를 추가하지 않고 기존 `performance/`를 기준으로 정리한다.

```text
새 perf/ 생성 X
기존 performance/ 확장 O
```

이유는 다음과 같다.

- 기존 발언권 k6 스크립트가 이미 `performance/k6` 아래에 있음
- 테스트 관련 산출물이 한곳에 모여야 함
- 팀원이 실행 위치를 혼동하지 않음

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
SPEECH_ID=1 \
USER_ID_BASE=100000 \
k6 run performance/k6/core-api-mixed.js
```

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
- Git commit
- 실행 명령
- 주요 지표
- 실패율
- 병목 추정
- 후속 작업
