# Playwright E2E 테스트

## 목적

프론트 화면이 백엔드 API 계약과 실제 사용자 흐름대로 동작하는지 검증한다.

검증 범위는 다음과 같다.

- 핵심 사용자 플로우
- 관리자 신고·제재 플로우
- 예외 메시지와 제한 정책
- 신뢰도·활동 등급·발언 시간 같은 시각 표시
- 동일 사용자의 메인 스테이지 의견 묶음 표시

## 실행 전 조건

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:3000`
- DB/Redis local 환경 실행
- 관리자 계정 존재
- Playwright 브라우저 설치

```bash
cd frontend
npx playwright install chromium
```

## 환경 변수

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `E2E_API_BASE_URL` | `http://localhost:8080` | 백엔드 API 주소 |
| `E2E_FRONTEND_BASE_URL` | `http://localhost:3000` | 프론트 주소 |
| `E2E_ADMIN_EMAIL` | 없음 | 관리자 이메일 |
| `E2E_ADMIN_PASSWORD` | 없음 | 관리자 비밀번호 |
| `E2E_TEST_PASSWORD` | `E2eTest123!` | 테스트 사용자 비밀번호 |
| `E2E_RUN_ID` | 현재 시각 기반 | 테스트 데이터 식별자 |

관리자 계정은 자동 생성하지 않는다.
권한 정책 검증을 위해 실제 관리자 계정으로 실행한다.

## 실행 순서

```bash
cd frontend

# 1. 테스트 데이터 생성
E2E_ADMIN_EMAIL=admin@example.com \
E2E_ADMIN_PASSWORD=password \
npm run e2e:prepare

# 2. E2E 실행
E2E_ADMIN_EMAIL=admin@example.com \
E2E_ADMIN_PASSWORD=password \
npm run e2e

# 3. 테스트 데이터 정리
E2E_ADMIN_EMAIL=admin@example.com \
E2E_ADMIN_PASSWORD=password \
npm run e2e:cleanup
```

## 테스트 데이터

`npm run e2e:prepare`는 다음 데이터를 생성한다.

- 일반 사용자 2명
- 승인 토픽 1개
- OPEN 토론방 1개
- 발언권 신청 1건
- 메인 의견 2개
- 의견 공감 1건
- 의견 신고 1건

생성된 ID는 `frontend/e2e/.e2e-state.json`에 저장된다.
이 파일은 로컬 실행 상태 파일이므로 커밋하지 않는다.

## 정리 정책

`npm run e2e:cleanup`은 다음 작업을 수행한다.

- 테스트 사용자를 토론방에서 퇴장 처리
- 테스트 토론방 종료
- 테스트 토픽 삭제 시도
- `.e2e-state.json` 삭제

회원 삭제 API는 현재 없으므로 테스트 사용자는 DB에 남을 수 있다.
반복 실행 시 이메일에 `E2E_RUN_ID`가 포함되므로 충돌하지 않는다.

## 시나리오

| 파일 | 검증 내용 |
| --- | --- |
| `user-flow.spec.ts` | 로그인, 토론방 진입, 신뢰도 표시, 발언 시간 표시, 의견 묶음 표시 |
| `admin-moderation.spec.ts` | 관리자 신고 목록, 검토 시작, 심각도 선택, 위반 확정, 제재 추천 UI |
| `policy-guard.spec.ts` | 비로그인 접근, 잘못된 로그인, 원시 enum 미노출 |

## 운영 원칙

- 테스트 데이터 생성과 삭제는 UI가 아니라 API로 처리한다.
- 사용자 경험 검증은 UI에서 수행한다.
- 제재 적용처럼 운영 데이터에 영향을 주는 동작은 기본 시나리오에서 실제 실행하지 않는다.
- 실제 제재 적용 E2E는 별도 격리 DB 또는 전용 테스트 환경에서만 수행한다.
