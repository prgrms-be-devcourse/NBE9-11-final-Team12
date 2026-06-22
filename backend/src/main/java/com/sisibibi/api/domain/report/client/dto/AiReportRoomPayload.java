package com.sisibibi.api.domain.report.client.dto;

import java.time.LocalDateTime;

public record AiReportRoomPayload(
        String title,
        LocalDateTime startedAt,
        LocalDateTime endedAt
) {
}
