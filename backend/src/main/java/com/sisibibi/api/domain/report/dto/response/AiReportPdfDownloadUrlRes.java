package com.sisibibi.api.domain.report.dto.response;

import java.time.Instant;

public record AiReportPdfDownloadUrlRes(
        String downloadUrl,
        Instant expiresAt
) {
}
