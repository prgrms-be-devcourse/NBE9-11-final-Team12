package com.sisibibi.api.domain.speech.dto;

import com.sisibibi.api.domain.speech.entity.SpeechStance;
import jakarta.validation.constraints.NotBlank;

public record SpeechCreateRequest(
        @NotBlank(message = "의견 내용은 비어 있을 수 없습니다.")
        String content,
        SpeechStance stance
) {

    public SpeechCreateCommand toCommand() {
        return new SpeechCreateCommand(content, stance);
    }
}
