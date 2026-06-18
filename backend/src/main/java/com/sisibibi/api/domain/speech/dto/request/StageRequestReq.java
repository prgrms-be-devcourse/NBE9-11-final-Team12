package com.sisibibi.api.domain.speech.dto.request;

import com.sisibibi.api.domain.speech.entity.SpeechStance;
import jakarta.validation.constraints.NotNull;

public record StageRequestReq(
        @NotNull SpeechStance stance
) {
}
