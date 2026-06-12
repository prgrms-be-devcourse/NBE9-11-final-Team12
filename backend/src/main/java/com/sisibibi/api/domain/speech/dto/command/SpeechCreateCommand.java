package com.sisibibi.api.domain.speech.dto.command;

import com.sisibibi.api.domain.speech.entity.SpeechStance;

public record SpeechCreateCommand(
        String content,
        SpeechStance stance
) {
}
