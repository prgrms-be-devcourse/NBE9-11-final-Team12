package com.sisibibi.api.domain.speech.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SpeechImageConfirmReq(
    @NotBlank(message = "이미지 키는 비어 있을 수 없습니다.")
    String imageKey
) {
}
