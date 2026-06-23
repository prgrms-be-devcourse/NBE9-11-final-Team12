# Spring Boot Prompt Guard Task Prompt

아래 프롬프트를 Spring Boot 저장소 작업자에게 전달하세요.

```text
프롬프트 인젝션 1차 방어를 Spring Boot에 구현해줘.

목표:
- AI 리포트 생성 요청에서 개인화 리포트용 customPrompts를 최대 5개까지 받는다.
- 기본 리포트는 customPrompts가 없거나 빈 배열이어도 된다.
- 개인화 리포트는 customPrompts 1~5개를 허용한다.
- 각 customPrompt는 사용자가 입력한 신뢰할 수 없는 데이터다.

요구사항:
1. 요청 DTO에 customPrompts 필드를 추가한다.
   - 형태 예시:
     customPrompts: [
       { "label": "custom 1", "prompt": "..." },
       { "label": "custom 2", "prompt": "..." }
     ]
   - 최대 5개
   - prompt는 null/blank 금지
   - 각 prompt 길이는 정책값으로 제한한다. 기본값은 1000자 권장
   - label은 없으면 서버에서 custom 1~custom 5로 정규화해도 된다.

2. 저장 전 검증을 수행한다.
   - 개수 초과 차단
   - 빈 prompt 차단
   - 너무 긴 prompt 차단
   - 제어문자, 비정상 Unicode, 과도한 공백은 정규화 또는 차단
   - 검증 실패 시 400 응답

3. Prompt Guard 서버를 호출한다.
   - POST {PROMPT_GUARD_BASE_URL}/scan
   - body:
     {
       "content": "<customPrompt>",
       "type": "analyze"
     }
   - 응답 matches의 severity 또는 action/blocked를 확인한다.
   - HIGH 또는 CRITICAL이면 요청을 차단한다.
   - MEDIUM 이하는 저장은 허용하되 audit log를 남긴다.
   - Prompt Guard 장애 시 정책은 fail-closed를 기본으로 한다.
   - 운영 설정으로 fail-open 전환 가능하게 한다.

4. 차단 응답에는 원문 prompt를 그대로 노출하지 않는다.
   - 예:
     {
       "code": "PROMPT_GUARD_BLOCKED",
       "message": "개인화 요청에 안전하지 않은 지시가 포함되어 있습니다.",
       "severity": "HIGH"
     }

5. DB에는 안전 판정된 customPrompts만 저장한다.
   - blocked된 prompt 원문은 저장하지 않는다.
   - 필요하면 hash, severity, reason만 audit 테이블에 저장한다.

6. 작업 발행 메시지에 customPrompts를 포함한다.
   - Python AI 서버가 다시 2차 검사를 수행할 수 있도록 원문 customPrompt를 전달한다.
   - 단, Spring에서 이미 차단된 요청은 발행하지 않는다.

7. 테스트를 추가한다.
   - customPrompts 6개면 400
   - 빈 prompt면 400
   - 너무 긴 prompt면 400
   - Prompt Guard HIGH/CRITICAL이면 차단
   - Prompt Guard SAFE/LOW/MEDIUM이면 저장 + 작업 발행
   - Prompt Guard 장애 시 fail-closed 설정이면 차단
```
