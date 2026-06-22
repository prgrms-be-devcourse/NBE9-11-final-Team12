package com.sisibibi.api.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageReq(
        @NotBlank(message = "메시지를 입력해주세요.")
        @Size(max = 300, message = "메시지는 최대 300자까지 입력할 수 있습니다.")
        String content
) {
}
