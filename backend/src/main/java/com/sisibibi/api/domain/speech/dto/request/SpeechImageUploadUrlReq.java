package com.sisibibi.api.domain.speech.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record SpeechImageUploadUrlReq(
    @NotBlank(message = "이미지 Content-Type은 비어 있을 수 없습니다.")
    String contentType,

    @Positive(message = "이미지 파일 크기는 0보다 커야 합니다.")
    long fileSize
) {
}