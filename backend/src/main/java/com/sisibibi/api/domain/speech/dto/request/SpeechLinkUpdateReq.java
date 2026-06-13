package com.sisibibi.api.domain.speech.dto.request;

import com.sisibibi.api.domain.speech.validation.HttpUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SpeechLinkUpdateReq(
        @NotBlank(message = "근거 링크는 비어 있을 수 없습니다.")
        @Size(max = 500, message = "근거 링크는 500자를 초과할 수 없습니다.")
        @HttpUrl
        String linkUrl
) {
}
