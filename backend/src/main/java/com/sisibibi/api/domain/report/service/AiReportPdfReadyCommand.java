package com.sisibibi.api.domain.report.service;

public record AiReportPdfReadyCommand(
        Long exportId,
        String recipientEmail,
        String recipientNickname,
        String roomTitle,
        String homepageUrl
) {
}
