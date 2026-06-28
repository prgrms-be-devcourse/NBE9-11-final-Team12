# Playwright E2E 테스트

## 목적

프론트 화면이 백엔드 API 계약과 실제 사용자 흐름대로 동작하는지 검증한다.

검증 범위는 다음과 같다.

- 핵심 사용자 플로우
- 관리자 신고·제재 플로우
- 예외 메시지와 제한 정책
- 신뢰도·활동 등급·발언 시간 같은 시각 표시
- 동일 사용자의 메인 스테이지 의견 표시

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
| `api-policy-flow.spec.ts` | Access/Refresh Token 재발급, 로그아웃, 의견·공감·신고 예외정책, 제재 제한, 계정 정지, 관리자 신고 검토 |
| `user-flow.spec.ts` | 로그인, 토론방 진입, 신뢰도 표시, 발언 시간 표시, 동일 작성자 의견 표시 |
| `complete-user-action-flow.spec.ts` | 브라우저 UI 기반 의견 작성, 공감, 의견 신고, 채팅 전송, 채팅 신고 |
| `admin-moderation.spec.ts` | 관리자 신고 목록, 검토 시작, 심각도 선택, 위반 확정, 제재 추천 UI |
| `policy-guard.spec.ts` | 비로그인 접근, 잘못된 로그인, 원시 enum 미노출 |

## 검증 기준

### 인증 흐름

- 로그인 성공 시 `accessToken`, `refreshToken` 쿠키가 발급되는지 확인한다.
- `accessToken`이 없을 때 보호 API가 `401`로 거절되는지 확인한다.
- `refreshToken`으로 `/api/v1/auth/reissue` 호출 시 새 인증 쿠키가 발급되는지 확인한다.
- 로그아웃 이후 보호 API 접근이 거절되는지 확인한다.

### 사용자 흐름

- 일반 사용자가 토론방에 입장하고 화면에서 신뢰도, 활동 등급, 발언 시간, 동일 작성자 의견 표시를 확인한다.
- 브라우저 UI에서 의견 작성 모달을 열고, 내용을 입력한 뒤 등록 결과가 화면에 표시되는지 확인한다.
- 브라우저 UI에서 다른 사용자가 의견 공감과 의견 신고를 수행하고 결과 모달을 확인한다.
- 브라우저 UI에서 채팅 메시지를 전송하고 다른 사용자 화면에서 재조회 후 확인한다.
- 브라우저 UI에서 채팅 메시지 신고 모달을 열고 신고 접수 결과를 확인한다.
- 의견 빈 내용, 욕설 포함, 본인 신고, 기타 사유 상세 설명 누락, 중복 신고, 중복 공감, 없는 공감 취소를 검증한다.
- 발언권·의견 제한 상태에서는 발언권 신청과 의견 작성이 거절되는지 확인한다.
- 계정 정지 상태에서는 기존 Access Token, Refresh Token 재발급, 신규 로그인이 모두 거절되는지 확인한다.

### 관리자 흐름

- 관리자가 신고 목록과 상세 내용을 조회한다.
- 신고를 `PENDING → REVIEWING → RESOLVED`로 처리한다.
- 위반 확정 시 심각도 누락이 거절되는지 확인한다.
- 처리 완료된 신고를 기준으로 제재 추천 조회가 가능한지 확인한다.

## 결과 기록 예시

```text
실행 명령:
E2E_ADMIN_EMAIL=local-admin@sisibibi.test E2E_ADMIN_PASSWORD=... npm run e2e

결과:
- 총 테스트: n개
- 성공: n개
- 실패: 0개
- 리포트: frontend/playwright-report/index.html

확인한 흐름:
- 인증 쿠키 발급, 재발급, 로그아웃
- 토론방 진입, 발언권, 의견, 공감, 신고
- 관리자 신고 검토, 제재 추천
- 계정 정지 후 기존 세션 차단
```

## 운영 원칙

- 테스트 데이터 생성과 삭제는 UI가 아니라 API로 처리한다.
- 사용자 경험 검증은 UI에서 수행한다.
- 제재 적용처럼 운영 데이터에 영향을 주는 동작은 기본 시나리오에서 실제 실행하지 않는다.
- 실제 제재 적용 E2E는 별도 격리 DB 또는 전용 테스트 환경에서만 수행한다.
