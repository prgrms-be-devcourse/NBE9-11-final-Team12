package com.sisibibi.api.domain.speech.dto.request;

import com.sisibibi.api.domain.speech.dto.command.SpeechUpdateCommand;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import jakarta.validation.constraints.NotBlank;

public record SpeechUpdateReq(
        @NotBlank(message = "의견 내용은 비어 있을 수 없습니다.")
        String content,
        SpeechStance stance
) {

    public SpeechUpdateCommand toCommand() {
        return new SpeechUpdateCommand(content, stance);
    }
}
