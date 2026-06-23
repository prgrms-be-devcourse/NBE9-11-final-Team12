SUPPORTED_STANCE_VALUES = ("PRO", "CON", "NEUTRAL")
MAX_CUSTOM_PROMPTS = 5
MAX_CUSTOM_PROMPT_LENGTH = 1000


def normalize_report_request(payload):
    # 백엔드 엔티티/DTO 전체를 LLM에 넘기지 않고, 리포트 생성에 필요한 최소 필드만 남깁니다.
    # 현재 백엔드 기준으로 Room, Topic, Speech에서 필요한 값만 읽고 나머지는 버립니다.
    room = payload.get("room") or {}
    topic = payload.get("topic") or {}
    speeches = payload.get("speeches") or payload.get("opinions") or []
    normalized_speeches = _normalize_speeches(speeches)

    normalized = {
        "room": _drop_empty_values({
            "topic": _first_text(topic.get("title"), room.get("topic"), room.get("title"), payload.get("topic")),
            "description": _first_text(topic.get("description"), room.get("description"), payload.get("description")),
            "startedAt": room.get("startedAt"),
            "endedAt": room.get("endedAt"),
            "totalSpeeches": len(normalized_speeches),
        }),
        "speeches": normalized_speeches,
    }
    custom_prompts = _normalize_custom_prompts(payload.get("customPrompts") or [])
    if custom_prompts:
        normalized["customPrompts"] = custom_prompts
    return normalized


def _normalize_custom_prompts(custom_prompts):
    if len(custom_prompts) > MAX_CUSTOM_PROMPTS:
        raise ValueError("customPrompts accepts at most 5 prompts")

    normalized = []
    for index, item in enumerate(custom_prompts, start=1):
        if isinstance(item, str):
            label = f"custom {index}"
            prompt = item
        else:
            label = _first_text(item.get("label"), f"custom {index}")
            prompt = item.get("prompt", "")

        compact_prompt = _compact_content(prompt)
        if not compact_prompt:
            raise ValueError(f"customPrompts[{index - 1}].prompt must not be blank")
        if len(compact_prompt) > MAX_CUSTOM_PROMPT_LENGTH:
            raise ValueError(
                f"customPrompts[{index - 1}].prompt must be {MAX_CUSTOM_PROMPT_LENGTH} characters or fewer"
            )

        normalized.append({
            "label": label,
            "prompt": compact_prompt,
        })

    return normalized


def _normalize_speeches(speeches):
    normalized = []
    for index, speech in enumerate(speeches, start=1):
        if speech.get("deleted") is True or speech.get("isDeleted") is True:
            continue

        content = _compact_content(speech.get("content", ""))
        stance = speech.get("stance")
        if not content or stance not in SUPPORTED_STANCE_VALUES:
            continue

        speech_id = speech.get("speechId", speech.get("id"))
        user_id = speech.get("userId")
        normalized.append(
            _drop_empty_values({
                "turnIndex": speech_id or index,
                "speaker": _speaker_name(user_id, speech.get("speaker")),
                "mergeKey": _merge_key(speech),
                "stance": stance,
                "content": content,
                "messageCount": speech.get("messageCount", 1),
                "keywords": speech.get("keywords") or [],
                "createdAt": _first_text(
                    speech.get("createdAt"),
                    speech.get("startedAt"),
                    speech.get("endedAt"),
                ),
            })
        )

    normalized = _merge_same_speaker_slot(normalized)
    normalized.sort(key=lambda speech: (str(speech.get("createdAt") or ""), speech["turnIndex"]))
    return [
        {
            "turnIndex": index,
            "speaker": speech["speaker"],
            "stance": speech["stance"],
            "content": speech["content"],
            "messageCount": speech["messageCount"],
            **({"keywords": speech["keywords"]} if speech.get("keywords") else {}),
        }
        for index, speech in enumerate(normalized, start=1)
    ]


def _compact_content(content):
    # 여러 메시지를 합친 content가 들어와도 공백을 정리해 토큰 낭비를 줄입니다.
    return " ".join(str(content or "").split())


def _merge_same_speaker_slot(speeches):
    merged = []
    indexes_by_key = {}

    for speech in speeches:
        merge_key = speech.get("mergeKey")
        if not merge_key:
            merged.append(speech)
            continue

        if merge_key not in indexes_by_key:
            indexes_by_key[merge_key] = len(merged)
            merged.append(speech)
            continue

        target = merged[indexes_by_key[merge_key]]
        target["content"] = _compact_content(f"{target['content']} {speech['content']}")
        target["messageCount"] = int(target.get("messageCount", 1)) + int(speech.get("messageCount", 1))
        if speech.get("keywords"):
            target["keywords"] = sorted(set(target.get("keywords", []) + speech["keywords"]))

    return merged


def _merge_key(speech):
    for key_name in ("turnId", "speakerTurnId", "speakingSlotId", "speakingQueueId"):
        explicit_key = speech.get(key_name)
        if explicit_key not in ("", None):
            return f"{key_name}:{explicit_key}"

    user_id = speech.get("userId")
    stance = speech.get("stance")
    started_at = speech.get("startedAt")
    ended_at = speech.get("endedAt")
    if user_id is not None and stance and started_at and ended_at:
        return f"{user_id}:{stance}:{started_at}:{ended_at}"
    return ""


def _speaker_name(user_id, fallback):
    if fallback:
        return str(fallback)
    if user_id is None:
        return "unknown"
    return f"user-{user_id}"


def _first_text(*values):
    for value in values:
        if isinstance(value, str) and value.strip():
            return value.strip()
    return ""


def _drop_empty_values(data):
    return {
        key: value
        for key, value in data.items()
        if value not in ("", None, [])
    }
