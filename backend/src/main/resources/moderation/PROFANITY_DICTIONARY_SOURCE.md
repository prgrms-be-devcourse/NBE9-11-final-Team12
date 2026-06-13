# Profanity Dictionary Source

## Upstream

- Repository: `VaneProject/bad-word-filtering`
- Source: `https://github.com/VaneProject/bad-word-filtering/blob/master/badwords.txt`
- License: MIT
- Copyright: `Copyright (c) 2023 PersesTitan`
- Reviewed: 2026-06-14

## Additional Review Sources

- `리그오브레전드_필터링리스트_2020.txt`: 사용자 제공 파일, 3,269개 고유 표현 검토
- `slang.csv`: 사용자 제공 파일, 4,314개 고유 표현 검토

두 파일은 재배포 라이선스를 확인할 수 없어 원본 전체를 저장소에 포함하지 않았다.
대표 우회 표현과 명백한 성적 괴롭힘 표현만 검수하여 파생 사전에 반영했다.

숫자 삽입과 제한된 한글·영문 혼합 우회는 사전 항목을 계속 복제하지 않고
탐지기의 다중 정규화 후보로 처리한다. 기본 문자열, 숫자를 제거한 문자열,
검수된 음차·두벌식 우회 치환 문자열을 각각 검사한다.

## Selection Policy

원본 목록을 그대로 사용하지 않고 명백한 욕설, 모욕, 욕설 우회 표기만 선별했다.

다음 항목은 자동 차단 대상에서 제외했다.

- 성소수자 또는 성 정체성을 나타내는 표현
- 성교육, 의학, 신체 부위를 설명할 때 정상적으로 사용할 수 있는 표현
- `공지`, `공지사항`, `운영자`, `마스터` 같은 서비스 일반 용어
- 특정 게임이나 서비스에서만 의미가 있는 고유명사
- 일상 문장에 자주 포함되어 부분 문자열 오탐 가능성이 높은 표현

성적 표현은 `sexual-harassment-phrases.txt`로 분리한다. `성교`, `성폭행`,
신체 부위 명칭처럼 교육·의학·범죄 토론에 필요한 단독 용어는 허용하고,
상대를 대상으로 한 성적 명령·비하 또는 노골적인 행위 표현만 차단한다.

외부 원본에서 가져온 표현과 서비스가 자체 추가한 표현은
`profanity-words.txt` 내부 주석으로 구분한다.

## Update Policy

- 신고 검수로 확인된 미탐 표현만 추가한다.
- 정상 문장 오탐이 확인되면 단어를 제거하거나 허용 표현에 추가한다.
- 외부 목록을 갱신할 때는 라이선스와 위 선택 기준을 다시 확인한다.
