package com.sisibibi.api.domain.speech.dto.command;

import com.sisibibi.api.domain.speech.entity.SpeechStance;

public record SpeechUpdateCommand(
        String content,
        SpeechStance stance
) {
}
