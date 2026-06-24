package com.sisibibi.api.domain.report.client.dto;

import com.sisibibi.api.domain.speech.entity.SpeechStance;

import java.time.LocalDateTime;

public record AiReportSpeechPayload(
        Long speechId,
        Long userId,
        SpeechStance stance,
        String content,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        LocalDateTime createdAt
) {
}
