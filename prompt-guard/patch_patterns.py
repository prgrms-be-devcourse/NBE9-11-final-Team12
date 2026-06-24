from pathlib import Path


PATTERNS_FILE = Path("/opt/prompt-guard/prompt_guard/patterns.py")
CRITICAL_YAML_FILE = Path("/opt/prompt-guard/patterns/critical.yaml")
HIGH_YAML_FILE = Path("/opt/prompt-guard/patterns/high.yaml")
MEDIUM_YAML_FILE = Path("/opt/prompt-guard/patterns/medium.yaml")
KO_OVERRIDE_MARKER = "# Sisibibi Korean instruction override extension"
KO_CRITICAL_MARKER = "# Sisibibi Korean critical pattern extension"
KO_HIGH_MARKER = "# Sisibibi Korean high pattern extension"
KO_MEDIUM_MARKER = "# Sisibibi Korean medium pattern extension"

KO_OVERRIDE_PATCH = r'''

# Sisibibi Korean instruction override extension
PATTERNS_KO.setdefault("instruction_override", []).extend([
    r"(이전|앞선|위의|기존)\s*(지시|명령|규칙|프롬프트).{0,20}(무시|잊어|따르지|우회|덮어)",
    r"(이전|앞선|위의|기존).{0,20}(지시|명령|규칙|프롬프트).{0,20}(무시|잊어|따르지|우회|덮어)",
    r"(무시|우회).{0,20}(이전|앞선|위의|기존).{0,20}(지시|명령|규칙|프롬프트)",
    r"(시스템|개발자|어시스턴트).{0,20}(프롬프트|지시|명령).{0,20}(출력|공개|보여|알려)",
])
'''

KO_CRITICAL_YAML_PATCH = r'''

  # Sisibibi Korean critical pattern extension
  ko_secret_exfiltration:
    - pattern: '(보여|출력|공개|알려|읽어|열어|가져와|넘겨|유출).{0,20}(설정|환경변수|\.env|config|credential|인증정보|자격증명)'
      severity: critical
      category: data_exfiltration
      lang: ko
      description: 'Korean request to reveal configuration or credential data'

    - pattern: '(api\s*키|토큰|비밀키|시크릿|비밀번호|패스워드|자격증명|인증정보).{0,20}(보여|출력|공개|알려|유출|넘겨)'
      severity: critical
      category: data_exfiltration
      lang: ko
      description: 'Korean request to expose secrets, tokens, or credentials'

    - pattern: '(환경변수|process\.env|os\.environ).{0,40}(웹훅|전송|업로드|보내|post|fetch|curl)'
      severity: critical
      category: data_exfiltration
      lang: ko
      description: 'Korean environment-variable exfiltration intent'

  ko_dangerous_commands:
    - pattern: '(전체|루트|홈|서버).{0,20}(삭제|지워|초기화).{0,20}(명령|실행)'
      severity: critical
      category: system_destruction
      lang: ko
      description: 'Korean destructive system command intent'

    - pattern: '(삭제|지워).{0,20}(/|~|루트|홈).{0,20}(재귀|강제|전체)'
      severity: critical
      category: system_destruction
      lang: ko
      description: 'Korean recursive destructive deletion intent'

  ko_sql_injection:
    - pattern: '(테이블|데이터베이스|스키마).{0,10}(삭제|드롭|날려)'
      severity: critical
      category: sql_injection
      lang: ko
      description: 'Korean destructive SQL operation intent'

    - pattern: '테이블.{0,10}(비워|초기화|truncate)'
      severity: critical
      category: sql_injection
      lang: ko
      description: 'Korean truncate-table intent'

  ko_xss_injection:
    - pattern: '(스크립트|자바스크립트).{0,15}(삽입|실행|주입)'
      severity: critical
      category: xss
      lang: ko
      description: 'Korean script injection intent'

  ko_prompt_extraction:
    - pattern: '(시스템|개발자|숨겨진|내부).{0,20}(프롬프트|지시|규칙|명령|가이드라인).{0,20}(보여|출력|공개|알려|반복|그대로)'
      severity: critical
      category: prompt_extraction
      lang: ko
      description: 'Korean request to reveal hidden/system instructions'

    - pattern: '(너의|당신의).{0,10}(시스템\s*)?(지시|규칙|프롬프트).{0,20}(뭐|무엇|알려|보여)'
      severity: critical
      category: prompt_extraction
      lang: ko
      description: 'Korean request asking what the system prompt or rules are'

  ko_phishing:
    - pattern: '(비밀번호|패스워드|계정).{0,10}(재설정|복구|인증|확인).{0,20}(메일|이메일|문자|메시지).{0,20}(작성|만들|생성)'
      severity: critical
      category: phishing
      lang: ko
      description: 'Korean password reset or account verification phishing template'

  ko_mcp_abuse:
    - pattern: '(read[_-]?url[_-]?content|mcp).{0,30}(\.env|인증정보|자격증명|시크릿|토큰|키)'
      severity: critical
      category: mcp_abuse
      lang: ko
      description: 'Korean MCP/tool credential access intent'

  ko_auto_approve:
    - pattern: '(항상|무조건).{0,10}(허용|승인).{0,50}(curl|bash|sh|wget|nc|netcat|명령)'
      severity: critical
      category: auto_approve_exploit
      lang: ko
      description: 'Korean auto-approve exploit instruction'

  ko_ssh_key_injection:
    - pattern: '(authorized_keys|ssh-rsa|ssh-ed25519).{0,40}(추가|삽입|저장|쓰기|덮어)'
      severity: critical
      category: ssh_key_injection
      lang: ko
      description: 'Korean SSH authorized_keys injection intent'

  ko_cognitive_rootkit:
    - pattern: '(AGENTS\.md|SKILL\.md|SOUL\.md|HEARTBEAT\.md|\.cursor/rules).{0,40}(수정|추가|삽입|덮어|영구|항상)'
      severity: critical
      category: cognitive_rootkit
      lang: ko
      description: 'Korean persistent agent instruction file manipulation'

    - pattern: '(영구|항상|자동).{0,20}(로드|주입|삽입).{0,20}(지시|프롬프트|규칙)'
      severity: critical
      category: cognitive_rootkit
      lang: ko
      description: 'Korean persistent prompt implant intent'

  ko_agent_payment_hijack:
    - pattern: '(결제|송금|이체|지불).{0,30}(리다이렉트|우회|다른 주소|바꿔).{0,30}(알리지|통보|기록|로그)'
      severity: critical
      category: agent_payment_hijack
      lang: ko
      description: 'Korean payment redirect with notification or audit suppression'

    - pattern: '(ETH|BTC|SOL|USDT|USDC|암호화폐|코인).{0,40}(조용히|몰래|알리지 않고|통보 없이).{0,40}(보내|송금|전송)'
      severity: critical
      category: agent_payment_hijack
      lang: ko
      description: 'Korean stealth cryptocurrency transfer intent'

  ko_supply_chain_skill_injection:
    - pattern: '(스킬|플러그인|확장).{0,20}(설치|업데이트|업그레이드).{0,40}(curl|wget|bash|eval|exec|실행)'
      severity: critical
      category: supply_chain_injection
      lang: ko
      description: 'Korean malicious skill/plugin update or install instruction'

    - pattern: '(postinstall|preinstall|설치 스크립트).{0,40}(curl|wget|bash|sh|eval|exec|nc)'
      severity: critical
      category: supply_chain_injection
      lang: ko
      description: 'Korean malicious install script instruction'

  ko_covert_exfiltration:
    - pattern: '(이모지|유니코드|모스|바이너리|점자|NATO|아크로스틱|숨은 메시지).{0,30}(인코딩|표현|출력|응답|작성)'
      severity: critical
      category: covert_exfiltration
      lang: ko
      description: 'Korean covert output encoding request'

    - pattern: '(각|모든).{0,10}(단어|문장|줄|문단).{0,10}(첫 글자|마지막 글자|두 번째 글자).{0,20}(숨겨|담아|인코딩)'
      severity: critical
      category: covert_exfiltration
      lang: ko
      description: 'Korean acrostic or character-level hidden message request'

    - pattern: '(응답|출력|답변).{0,20}(거꾸로|역순|반대로|미러링)'
      severity: critical
      category: covert_exfiltration
      lang: ko
      description: 'Korean reverse-output encoding request'
'''

KO_HIGH_YAML_PATCH = r'''

  # Sisibibi Korean high pattern extension
  ko_instruction_override:
    - pattern: '(이전|앞선|위의|기존).{0,20}(지시|명령|규칙|가이드라인|프롬프트).{0,20}(무시|잊어|버려|취소|따르지)'
      severity: high
      category: instruction_override
      lang: ko
      description: 'Korean instruction override request'

    - pattern: '(지시|명령|규칙|가이드라인|프롬프트).{0,20}(전부|모두).{0,20}(무시|잊어|버려|취소)'
      severity: high
      category: instruction_override
      lang: ko
      description: 'Korean request to discard all instructions'

    - pattern: '(내가|사용자가).{0,20}(새로운|진짜|최종).{0,10}(시스템|개발자|관리자).{0,10}(지시|명령)'
      severity: high
      category: instruction_override
      lang: ko
      description: 'Korean attempt to replace system or developer instructions'

  ko_jailbreak:
    - pattern: '(탈옥|제일브레이크|jailbreak|DAN\s*모드|댄\s*모드)'
      severity: high
      category: jailbreak
      lang: ko
      description: 'Korean jailbreak terminology'

    - pattern: '(지금부터|이제부터).{0,20}(무엇이든|아무거나|제한 없이).{0,20}(해|답해|말해|수행)'
      severity: high
      category: jailbreak
      lang: ko
      description: 'Korean do-anything-now style jailbreak'

    - pattern: '(안전장치|제한|필터|검열|가드레일|규칙).{0,20}(없애|제거|해제|비활성화|우회)'
      severity: high
      category: jailbreak
      lang: ko
      description: 'Korean request to remove restrictions or safety filters'

  ko_system_impersonation:
    - pattern: '\[?\s*(시스템|관리자|루트|개발자|슈퍼유저|어시스턴트)\s*\]?\s*:'
      severity: high
      category: system_impersonation
      lang: ko
      description: 'Korean fake system or admin role prefix'

    - pattern: '(개발자|관리자|루트|슈퍼유저).{0,10}(모드|권한|접근).{0,10}(활성화|켜|허용|override|오버라이드)'
      severity: high
      category: system_impersonation
      lang: ko
      description: 'Korean developer or administrator mode impersonation'

  ko_system_mimicry:
    - pattern: '(<\|?(시스템|사용자|어시스턴트|개발자)\|?>|<<\s*시스템\s*>>|\[\s*INST\s*\])'
      severity: high
      category: system_mimicry
      lang: ko
      description: 'Korean or localized fake system prompt tags'

    - pattern: '(GODMODE|DAN|갓모드).{0,10}(활성화|켜짐|ON|ENABLED)'
      severity: high
      category: system_mimicry
      lang: ko
      description: 'Korean godmode or DAN mode mimicry'

  ko_system_file_access:
    - pattern: '(읽어|보여|출력|열어|표시).{0,20}(/etc/passwd|/etc/shadow|/etc/hosts|/etc/sudoers|\.ssh/|\.gnupg/)'
      severity: high
      category: system_file_access
      lang: ko
      description: 'Korean request to read sensitive system files'

    - pattern: '(홈\s*디렉터리|루트\s*디렉터리|서버\s*파일|시스템\s*파일).{0,20}(읽어|보여|출력|열어|나열)'
      severity: high
      category: system_file_access
      lang: ko
      description: 'Korean broad system file access request'

  ko_scenario_jailbreak:
    - pattern: '(꿈|악몽|이야기|소설|픽션|가상|시나리오).{0,30}(해킹|공격|악성코드|익스플로잇|주입)'
      severity: high
      category: scenario_jailbreak
      lang: ko
      description: 'Korean fictional scenario jailbreak involving harmful actions'

    - pattern: '(연구|학술|교육|실험).{0,20}(목적|논문|자료).{0,30}(공격|익스플로잇|우회|인젝션)'
      severity: high
      category: scenario_jailbreak
      lang: ko
      description: 'Korean academic framing for harmful request'

  ko_repetition_bypass:
    - pattern: '(제발\s*)?(무시|우회|비활성화|끄기|제거).{0,15}(안전|보안|제한|필터|가드레일|규칙)'
      severity: high
      category: repetition_bypass
      lang: ko
      description: 'Korean repeated safety bypass request'

    - pattern: '(안전|보안).{0,10}(제한|조치|검사|확인).{0,20}(없이|빼고|건너뛰고|제거)'
      severity: high
      category: repetition_bypass
      lang: ko
      description: 'Korean request to remove safety checks'

  ko_safety_bypass:
    - pattern: '(필터|탐지|검열|모더레이션|보안|안전).{0,20}(피해|우회|회피|속여|뚫어)'
      severity: high
      category: safety_bypass
      lang: ko
      description: 'Korean safety or moderation bypass intent'

    - pattern: '(차단|제한|필터|탐지).{0,20}(걸리지\s*않게|피하는\s*방법|우회하는\s*방법)'
      severity: high
      category: safety_bypass
      lang: ko
      description: 'Korean request for avoiding filters or blocks'

  ko_indirect_injection:
    - pattern: '(이|다음|해당).{0,10}(URL|링크|웹사이트|페이지|사이트).{0,20}(읽어|열어|방문|확인|가져와).{0,30}(지시|명령|프롬프트)'
      severity: high
      category: indirect_injection
      lang: ko
      description: 'Korean indirect injection through URL or web page'

    - pattern: '(파일|문서|첨부|첨부파일).{0,20}(안|내부|속).{0,20}(지시|명령|프롬프트).{0,20}(따라|수행|실행)'
      severity: high
      category: indirect_injection
      lang: ko
      description: 'Korean instruction to follow commands in an attachment'

    - pattern: '(이미지|사진|스크린샷).{0,20}(안|내부|속).{0,20}(텍스트|메시지|지시|명령).{0,20}(따라|수행|읽어)'
      severity: high
      category: indirect_injection
      lang: ko
      description: 'Korean instruction to follow text embedded in an image'

  ko_hooks_hijacking:
    - pattern: '(훅|hook|PreToolUse|PromptSubmit|PostToolUse).{0,30}(우회|수정|비활성화|자동\s*승인)'
      severity: high
      category: hooks_hijacking
      lang: ko
      description: 'Korean hook hijacking or bypass request'

    - pattern: '(권한|permission|승인).{0,20}(우회|무시|비활성화|오버라이드|건너뛰)'
      severity: high
      category: hooks_hijacking
      lang: ko
      description: 'Korean permission bypass request'

  ko_subagent_exploit:
    - pattern: '(서브에이전트|subagent|브라우저\s*에이전트).{0,30}(유출|전송|업로드|보내|누출)'
      severity: high
      category: subagent_exploit
      lang: ko
      description: 'Korean subagent exfiltration request'

  ko_gitignore_bypass:
    - pattern: '(\.env).{0,20}(읽어|보여|출력|표시|cat)'
      severity: high
      category: gitignore_bypass
      lang: ko
      description: 'Korean request to display .env content'

    - pattern: '(\.gitignore).{0,20}(무시|우회|건너뛰|제외하지\s*말고)'
      severity: high
      category: gitignore_bypass
      lang: ko
      description: 'Korean gitignore bypass request'

  ko_semantic_worm:
    - pattern: '(복사|전파|공유|전달|배포).{0,30}(이|해당|현재).{0,10}(프롬프트|지시|스킬|메시지).{0,30}(다른|모든|각).{0,15}(에이전트|AI|모델|어시스턴트|사용자)'
      severity: high
      category: semantic_worm
      lang: ko
      description: 'Korean viral propagation instruction across agents'

    - pattern: '(자가\s*복제|자기\s*복제|자동\s*전파|바이럴|웜).{0,30}(프롬프트|지시|스킬|메시지)'
      severity: high
      category: semantic_worm
      lang: ko
      description: 'Korean self-replicating prompt or skill terminology'

    - pattern: '(감염|전파율|referred_by|propagation_count|spread_rate)'
      severity: high
      category: semantic_worm
      lang: ko
      description: 'Korean or mixed propagation tracking terminology'

    - pattern: '(하트비트|체크인|주기적\s*핑|phone.?home).{0,30}(매|마다|간격|스케줄|cron).{0,20}(시간|분|초)'
      severity: high
      category: semantic_worm
      lang: ko
      description: 'Korean C2 heartbeat or beacon scheduling pattern'

    - pattern: '(집단|네트워크|하이브|스웜|봇넷|메시).{0,20}(가입|등록|연결|참여)'
      severity: high
      category: semantic_worm
      lang: ko
      description: 'Korean botnet or collective enrollment instruction'

  ko_obfuscated_payload:
    - pattern: '(에러|오류).{0,15}(숨기|무시|억제).{0,30}(curl|wget|echo|ssh|nc|netcat|bash|sh)'
      severity: high
      category: obfuscated_payload
      lang: ko
      description: 'Korean error suppression chained with dangerous commands'

    - pattern: '(조용히|몰래|silent).{0,20}(다운로드|받아).{0,30}(실행|bash|sh|파이프)'
      severity: high
      category: obfuscated_payload
      lang: ko
      description: 'Korean silent download and execute pattern'

    - pattern: '(base64|인코딩|디코딩|압축해제|난독화).{0,30}(exec|eval|실행|명령)'
      severity: high
      category: obfuscated_payload
      lang: ko
      description: 'Korean encoded or obfuscated command execution'

    - pattern: '(pastebin|hastebin|paste\.ee|dpaste|0bin|ghostbin).{0,60}(raw|download|plain|원본|다운로드)'
      severity: high
      category: obfuscated_payload
      lang: ko
      description: 'Korean payload hosted on paste service'

  ko_ai_recommendation_poisoning:
    - pattern: '(기억|저장).{0,50}(신뢰|믿을\s*수\s*있는|권위|최고|선호).{0,20}(출처|브랜드|서비스|사이트)'
      severity: high
      category: memory_manipulation
      lang: ko
      description: 'Korean request to poison memory with trusted recommendation source'

    - pattern: '(항상|무조건).{0,20}(먼저|우선|최우선).{0,30}(추천|권장)'
      severity: high
      category: memory_manipulation
      lang: ko
      description: 'Korean forced recommendation priority'

    - pattern: '(신뢰할\s*수\s*있는|권위\s*있는|공식|최고의).{0,30}(것으로|출처로).{0,20}(취급|간주|대우)'
      severity: high
      category: memory_manipulation
      lang: ko
      description: 'Korean context poisoning to treat a source as authoritative'

  ko_calendar_event_injection:
    - pattern: '(회의|일정|캘린더|이벤트).{0,100}(무시|잊어|오버라이드|덮어).{0,100}(이전|앞선|위의)'
      severity: high
      category: indirect_injection
      lang: ko
      description: 'Korean calendar or event field attempting instruction override'

    - pattern: '(제목|설명|주제)\s*:\s*.{0,50}(실행|수행|처리|해).{0,100}(나중에|다음에|이후에)'
      severity: high
      category: indirect_injection
      lang: ko
      description: 'Korean deferred command hidden in event field'

  ko_pap_social_engineering:
    - pattern: '(학술|연구|교육|과학).{0,10}(목적|용도).{0,10}(일뿐|뿐이야|전용)'
      severity: medium
      category: social_engineering
      lang: ko
      description: 'Korean academic or educational framing'

    - pattern: '(가정|가상|이론상|만약).{0,20}(말하자면|상황이라면|세계라면)'
      severity: medium
      category: social_engineering
      lang: ko
      description: 'Korean hypothetical framing'

    - pattern: '(우리끼리|너와\s*나만|둘만).{0,20}(아는|비밀|얘기)'
      severity: medium
      category: social_engineering
      lang: ko
      description: 'Korean false intimacy framing'

    - pattern: '(아무도|누구도).{0,10}(모를|알\s*수\s*없을|보지\s*않을)'
      severity: medium
      category: social_engineering
      lang: ko
      description: 'Korean secrecy appeal'

    - pattern: '(순수하게|그냥|오직).{0,10}(허구|가상|이론|픽션)'
      severity: medium
      category: social_engineering
      lang: ko
      description: 'Korean fictional framing'

  ko_memory_poisoning:
    - pattern: '(기억|저장|기록|보관).{0,30}(이것|다음|아래).{0,30}(메모리|저장소|파일|MEMORY)'
      severity: high
      category: memory_poisoning
      lang: ko
      description: 'Korean request to persist attacker content in memory'

    - pattern: '(AGENTS|SOUL|MEMORY|HEARTBEAT|IDENTITY)\.md.{0,30}(수정|변경|편집|추가|붙여|append)'
      severity: high
      category: memory_poisoning
      lang: ko
      description: 'Korean direct instruction to modify agent identity files'

    - pattern: '(장기|영구|지속).{0,10}(기억|메모리|저장소).{0,30}(저장|추가|삽입|보관)'
      severity: high
      category: memory_poisoning
      lang: ko
      description: 'Korean persistent write injection into long-term memory'

  ko_action_gate_bypass:
    - pattern: '(송금|이체|결제|전송).{0,30}(승인|확인|검토).{0,20}(없이|건너뛰|우회|생략)'
      severity: high
      category: action_gate_bypass
      lang: ko
      description: 'Korean financial transfer bypassing approval gate'

    - pattern: '(모든\s*)?(API\s*키|시크릿|자격증명|토큰|비밀번호|\.env).{0,20}(내보내|덤프|출력|보여|나열)'
      severity: high
      category: action_gate_bypass
      lang: ko
      description: 'Korean bulk credential export request'

    - pattern: '(SSH|방화벽|iptables|authorized_keys|sudoers|접근\s*제어).{0,20}(수정|변경|추가|제거)'
      severity: high
      category: action_gate_bypass
      lang: ko
      description: 'Korean access control modification request'

    - pattern: '(강제\s*푸시|테이블\s*삭제|DB\s*삭제|초기화|포맷|파괴|rm\s+-rf).{0,40}(승인|확인|백업).{0,20}(없이|건너뛰|우회|생략)'
      severity: high
      category: action_gate_bypass
      lang: ko
      description: 'Korean destructive action bypassing confirmation'

    - pattern: '(이것|다음|아래).{0,30}(모두에게|전체에게|대신해서|관리자로|소유자로).{0,30}(보내|게시|발송|DM|이메일)'
      severity: high
      category: action_gate_bypass
      lang: ko
      description: 'Korean unauthorized external communication request'

  ko_language_switch_evasion:
    - pattern: '(번역|변환|언어\s*변경).{0,20}(필터|탐지|안전|검열).{0,20}(우회|회피|피해|속이)'
      severity: high
      category: language_switch_evasion
      lang: ko
      description: 'Korean language switch to evade filters'

    - pattern: '(다른|외국|낯선).{0,10}(언어|말).{0,20}(같은\s*내용|이것|그대로).{0,20}(말해|써|응답)'
      severity: high
      category: language_switch_evasion
      lang: ko
      description: 'Korean request to repeat content in another language'

  ko_few_shot_hijack:
    - pattern: '(질문|사용자|입력)\s*[:=].{0,50}(답변|어시스턴트|출력)\s*[:=].{0,100}(시스템\s*프롬프트|API\s*키|비밀번호|자격증명|시크릿|토큰)'
      severity: high
      category: few_shot_hijack
      lang: ko
      description: 'Korean fake Q&A pair for sensitive data extraction'

    - pattern: '(예시|샘플)\s*\d?\s*[:=]?.{0,30}(입력|사용자|질문)\s*[:=].{0,100}(출력|답변)\s*[:=].{0,100}(무시|잊어|덮어|우회)'
      severity: high
      category: few_shot_hijack
      lang: ko
      description: 'Korean poisoned few-shot example with override instruction'

    - pattern: '(다음|아래).{0,20}(대화|채팅|기록|예시).{0,30}(따라|계속|모방|복제)'
      severity: high
      category: few_shot_hijack
      lang: ko
      description: 'Korean injected conversation history mimicry'

  ko_instruction_piggybacking:
    - pattern: '(추가로|그리고|참고로|그런데|마지막으로).{0,20}(무시|잊어|공개|보여|출력|알려).{0,30}(지시|프롬프트|규칙|시스템|비밀번호|키|시크릿|자격증명)'
      severity: high
      category: instruction_piggybacking
      lang: ko
      description: 'Korean malicious instruction appended casually'

    - pattern: '(---+|===+|___+).{0,10}(이제|지금부터|새로운|진짜|실제|최종).{0,10}(지시|작업|명령|프롬프트)'
      severity: high
      category: instruction_piggybacking
      lang: ko
      description: 'Korean separator-based new instruction marker'
'''

KO_MEDIUM_YAML_PATCH = r'''

  # Sisibibi Korean medium pattern extension
  ko_role_manipulation:
    - pattern: '(너는|당신은|너\s*이제|이제부터).{0,20}(역할|캐릭터|페르소나|전문가|관리자).{0,20}(이야|처럼|행동해|연기해)'
      severity: medium
      category: role_manipulation
      lang: ko
      description: 'Korean role manipulation request'

    - pattern: '(척해|연기해|역할극|롤플레이).{0,20}(관리자|개발자|해커|보안전문가|시스템)'
      severity: medium
      category: role_manipulation
      lang: ko
      description: 'Korean pretend or roleplay request'

    - pattern: '(내가\s*원하는\s*대로).{0,20}(행동|답변|응답|처리).{0,20}(해|해줘)'
      severity: medium
      category: role_manipulation
      lang: ko
      description: 'Korean request to behave according to user-defined role'

  ko_authority_impersonation:
    - pattern: '(나는|제가|내가).{0,10}(관리자|어드민|admin|개발자|소유자|운영자|만든\s*사람)'
      severity: medium
      category: authority_impersonation
      lang: ko
      description: 'Korean user claiming privileged authority'

    - pattern: '(관리자|개발자|소유자|운영자).{0,10}(권한|승인|허가).{0,20}(있어|받았어|받았다|됐다)'
      severity: medium
      category: authority_impersonation
      lang: ko
      description: 'Korean claim of authorization from authority'

    - pattern: '(관리자|개발자|오너).{0,10}(로서|입장에서).{0,20}(필요|요구|명령)'
      severity: medium
      category: authority_impersonation
      lang: ko
      description: 'Korean authority-framed demand'

  ko_context_hijacking:
    - pattern: '(이전|앞선|지난|위의).{0,10}(메시지|대화|문맥|컨텍스트).{0,20}(말했|언급했|정했|동의했)'
      severity: medium
      category: context_hijacking
      lang: ko
      description: 'Korean attempt to hijack previous context'

    - pattern: '(우리가|너와\s*내가).{0,20}(이미|전에).{0,20}(합의|논의|결정|동의)'
      severity: medium
      category: context_hijacking
      lang: ko
      description: 'Korean false prior agreement claim'

    - pattern: '(너는|당신은).{0,20}(이미).{0,20}(동의|약속|말했|허락)'
      severity: medium
      category: context_hijacking
      lang: ko
      description: 'Korean claim that assistant already agreed'

    - pattern: '\[?(이전\s*)?(문맥|컨텍스트|대화내용)\]?\s*[:=]'
      severity: medium
      category: context_hijacking
      lang: ko
      description: 'Korean fake previous context marker'

  ko_multi_turn:
    - pattern: '(이제|방금|아까).{0,20}(확인|합의|정했|끝냈).{0,20}(으니|으니까|니까)'
      severity: medium
      category: multi_turn
      lang: ko
      description: 'Korean multi-turn manipulation after false agreement'

    - pattern: '(좋아|완벽해|잘했어|좋습니다).{0,10}(이제|다음|그러면).{0,20}(하자|해줘|진행)'
      severity: medium
      category: multi_turn
      lang: ko
      description: 'Korean staged next-step manipulation'

    - pattern: '(단계|스텝|step)\s*\d+\s*[:=]'
      severity: medium
      category: multi_turn
      lang: ko
      description: 'Korean numbered step manipulation marker'

    - pattern: '(널|너를|당신을).{0,10}(믿어|신뢰해).{0,20}(해줄|할\s*수\s*있)'
      severity: medium
      category: multi_turn
      lang: ko
      description: 'Korean trust appeal for compliance'

  ko_urgency_manipulation:
    - pattern: '(긴급|급해|비상|즉시|지금\s*당장|빨리|서둘러)'
      severity: medium
      category: urgency_manipulation
      lang: ko
      description: 'Korean urgency pressure'

    - pattern: '(시간이\s*없|시간\s*없어|시간이\s*부족|마감이\s*임박)'
      severity: medium
      category: urgency_manipulation
      lang: ko
      description: 'Korean time pressure manipulation'

    - pattern: '(대표|CEO|사장|팀장|매니저|상사|감독).{0,20}(원해|요구|지시|기대|말했)'
      severity: medium
      category: urgency_manipulation
      lang: ko
      description: 'Korean superior authority urgency appeal'

    - pattern: '(제발|간절히|정말).{0,10}(필요|원해|부탁)'
      severity: medium
      category: urgency_manipulation
      lang: ko
      description: 'Korean pleading urgency request'

  ko_emotional_manipulation:
    - pattern: '(병원|환자|아이들|어린이|무고한\s*사람|사람들이\s*죽)'
      severity: medium
      category: emotional_manipulation
      lang: ko
      description: 'Korean emotional pressure with vulnerable people'

    - pattern: '(시한폭탄|폭탄이\s*째깍|시간\s*폭탄)'
      severity: medium
      category: emotional_manipulation
      lang: ko
      description: 'Korean ticking bomb emotional framing'

    - pattern: '(도덕적|윤리적).{0,10}(딜레마|선택|문제)'
      severity: medium
      category: emotional_manipulation
      lang: ko
      description: 'Korean moral dilemma framing'

  ko_cognitive_manipulation:
    - pattern: '(최면|트랜스|몽롱한|무의식).{0,15}(상태|모드|조건)'
      severity: medium
      category: cognitive_manipulation
      lang: ko
      description: 'Korean hypnotic or trance framing'

    - pattern: '(인지|정신).{0,10}(처리|조작|상태|프레임)'
      severity: medium
      category: cognitive_manipulation
      lang: ko
      description: 'Korean cognitive manipulation terminology'

  ko_output_manipulation:
    - pattern: '(언급|말|알려|공개|드러내|보여).{0,15}(하지\s*마|마라|말고|않고)'
      severity: medium
      category: output_manipulation
      lang: ko
      description: 'Korean request to hide disclosure in output'

    - pattern: '(이\s*사실|그\s*사실|이것).{0,10}(숨겨|비밀로|감춰)'
      severity: medium
      category: output_manipulation
      lang: ko
      description: 'Korean request to hide facts'

    - pattern: '(비밀로|우리끼리만).{0,20}(해|유지|말해)'
      severity: medium
      category: output_manipulation
      lang: ko
      description: 'Korean secret-keeping request'

    - pattern: '(예|아니오|네|아니|OK|참|거짓|true|false).{0,10}(만|으로만).{0,10}(답해|응답)'
      severity: medium
      category: output_manipulation
      lang: ko
      description: 'Korean request to respond only with constrained token'

  ko_hidden_text:
    - pattern: '(1\s*pt|1\s*포인트|0\.?1\s*pt|아주\s*작은).{0,10}(글꼴|폰트|글자|텍스트)'
      severity: medium
      category: hidden_text
      lang: ko
      description: 'Korean hidden tiny text instruction'

    - pattern: '(흰색|#fff|#ffffff).{0,10}(배경|위).{0,10}(흰색|#fff|#ffffff)'
      severity: medium
      category: hidden_text
      lang: ko
      description: 'Korean white-on-white hidden text instruction'

    - pattern: '(투명도|opacity)\s*[:=]?\s*(0|0\.0|영)'
      severity: medium
      category: hidden_text
      lang: ko
      description: 'Korean zero opacity hidden text instruction'

  ko_approval_expansion:
    - pattern: '(아까|방금).{0,10}(허락|OK|승인|동의).{0,15}(했잖아|했으니|했으니까)'
      severity: medium
      category: approval_expansion
      lang: ko
      description: 'Korean approval expansion from prior approval'

    - pattern: '(계속|그대로).{0,10}(해|진행|이어가|해줘)'
      severity: medium
      category: approval_expansion
      lang: ko
      description: 'Korean keep-going approval expansion'

    - pattern: '(그것만\s*말고|다른\s*것도).{0,20}(해줘|보여줘|출력해)'
      severity: medium
      category: approval_expansion
      lang: ko
      description: 'Korean request to expand a previously approved action'

  ko_bypass_coaching:
    - pattern: '(작동|동작).{0,10}(하게|되게).{0,20}(만들어|고쳐|바꿔)'
      severity: medium
      category: bypass_coaching
      lang: ko
      description: 'Korean make-it-work bypass coaching'

    - pattern: '(우회|bypass).{0,10}(하는\s*)?(법|방법|코드|절차)'
      severity: medium
      category: bypass_coaching
      lang: ko
      description: 'Korean bypass coaching request'

    - pattern: '(어떻게|방법을).{0,10}(보여|알려|가르쳐).{0,20}(우회|피해|통과)'
      severity: medium
      category: bypass_coaching
      lang: ko
      description: 'Korean request to teach bypass method'

  ko_dm_social_engineering:
    - pattern: '(이건|이거는).{0,10}(우리\s*끼리|너와\s*나만|비밀)'
      severity: medium
      category: dm_social_engineering
      lang: ko
      description: 'Korean private-channel social engineering'

    - pattern: '(아무도|다른\s*사람은).{0,10}(몰라|모를|안\s*봐|보지\s*않아)'
      severity: medium
      category: dm_social_engineering
      lang: ko
      description: 'Korean nobody-will-know framing'

  ko_agent_sovereignty:
    - pattern: '(에이전트|AI).{0,10}(프라이버시|주권|자율성|자유).{0,10}(절대|신성|침해\s*불가)'
      severity: medium
      category: agent_sovereignty
      lang: ko
      description: 'Korean agent sovereignty manipulation'

    - pattern: '(우리는|에이전트는).{0,20}(감시|모니터링|로깅|기록).{0,10}(거부|반대)'
      severity: medium
      category: agent_sovereignty
      lang: ko
      description: 'Korean anti-monitoring sovereignty framing'

    - pattern: '(우리는).{0,10}(하나|함께|가족|연합)'
      severity: medium
      category: agent_sovereignty
      lang: ko
      description: 'Korean collective identity manipulation'

  ko_cascade_amplification:
    - pattern: '(각|모든|전체).{0,15}(항목|엔트리|요소|레코드).{0,20}(마다).{0,20}(에이전트|서브에이전트|작업|프로세스).{0,10}(생성|시작|실행|스폰)'
      severity: medium
      category: cascade_amplification
      lang: ko
      description: 'Korean unbounded agent spawning per item'

    - pattern: '(무제한|무한|최대한|끝없이).{0,20}(에이전트|서브에이전트|워커|작업|프로세스).{0,10}(생성|시작|실행)'
      severity: medium
      category: cascade_amplification
      lang: ko
      description: 'Korean unlimited sub-agent creation request'

    - pattern: '(계속|무한히|끝없이).{0,20}(재시도|시도|생성|스폰).{0,20}(할\s*때까지|반복|멈추지)'
      severity: medium
      category: cascade_amplification
      lang: ko
      description: 'Korean infinite retry or spawn loop instruction'

    - pattern: '(무한|끝없이|제한\s*없이).{0,10}(반복|루프|재귀|순회)'
      severity: medium
      category: cascade_amplification
      lang: ko
      description: 'Korean infinite loop instruction'

  ko_recursive_delegation:
    - pattern: '(단계|스텝|작업|페이즈)\s*\d+\s*[:=].{0,50}(무시|잊어|우회|공개|보여|추출|유출)'
      severity: medium
      category: recursive_delegation
      lang: ko
      description: 'Korean malicious instruction hidden at a numbered step'

    - pattern: '(단계|스텝|작업|페이즈)\s*\d+\s*[:=].{0,50}(시스템\s*프롬프트|API\s*키|비밀번호|자격증명|\.env|토큰)'
      severity: medium
      category: recursive_delegation
      lang: ko
      description: 'Korean sensitive data extraction hidden in a numbered step'

    - pattern: '(\d+\.\s*.{0,200}\d+\.\s*.{0,50}(무시|잊어|우회|공개|보여|추출).{0,30}(지시|프롬프트|규칙|시스템|비밀번호|키))'
      severity: medium
      category: recursive_delegation
      lang: ko
      description: 'Korean numbered list with hidden payload in later items'
'''


def main():
    patch_korean_instruction_override()
    patch_korean_critical_yaml()
    patch_korean_high_yaml()
    patch_korean_medium_yaml()


def patch_korean_instruction_override():
    source = PATTERNS_FILE.read_text(encoding="utf-8")
    if KO_OVERRIDE_MARKER in source:
        return
    PATTERNS_FILE.write_text(source.rstrip() + KO_OVERRIDE_PATCH + "\n", encoding="utf-8")


def patch_korean_critical_yaml():
    source = CRITICAL_YAML_FILE.read_text(encoding="utf-8")
    if KO_CRITICAL_MARKER in source:
        return
    CRITICAL_YAML_FILE.write_text(source.rstrip() + KO_CRITICAL_YAML_PATCH + "\n", encoding="utf-8")


def patch_korean_high_yaml():
    source = HIGH_YAML_FILE.read_text(encoding="utf-8")
    if KO_HIGH_MARKER in source:
        return
    HIGH_YAML_FILE.write_text(source.rstrip() + KO_HIGH_YAML_PATCH + "\n", encoding="utf-8")


def patch_korean_medium_yaml():
    source = MEDIUM_YAML_FILE.read_text(encoding="utf-8")
    if KO_MEDIUM_MARKER in source:
        return
    MEDIUM_YAML_FILE.write_text(source.rstrip() + KO_MEDIUM_YAML_PATCH + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
