package com.sisibibi.api.domain.speech.dto.response;

import java.time.Instant;

public record SpeechImageUploadUrlRes(
    String uploadUrl,
    String imageUrl,
    String imageKey,
    Instant expiresAt
) {
}
