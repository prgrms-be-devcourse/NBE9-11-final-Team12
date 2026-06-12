package com.sisibibi.api.domain.speech.dto.request;

import com.sisibibi.api.domain.speech.entity.SpeechStance;

public record SpeechCreateCommand(
        String content,
        SpeechStance stance
) {
}
