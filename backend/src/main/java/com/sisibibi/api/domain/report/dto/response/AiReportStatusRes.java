package com.sisibibi.api.domain.report.dto.response;

import java.time.LocalDateTime;

public record AiReportStatusRes(
        Long roomId,
        Long reportId,
        String reportStatus,
        AiReportPdfStatusRes pdf,
        LocalDateTime requestedAt,
        LocalDateTime completedAt
) {
}
