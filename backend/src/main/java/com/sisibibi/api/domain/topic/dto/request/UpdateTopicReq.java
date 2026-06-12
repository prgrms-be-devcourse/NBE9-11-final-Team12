package com.sisibibi.api.domain.topic.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateTopicReq(
    @NotBlank
    String title,

    String description,

    @NotBlank
    String category,

    String sourceUrl
) {
}
