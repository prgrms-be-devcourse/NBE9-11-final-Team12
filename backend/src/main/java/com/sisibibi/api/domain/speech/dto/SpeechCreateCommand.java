package com.sisibibi.api.domain.speech.dto;

import com.sisibibi.api.domain.speech.entity.SpeechStance;

public record SpeechCreateCommand(
        String content,
        SpeechStance stance
) {
}
