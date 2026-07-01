package com.sisibibi.api.domain.report.dto.response;

import com.sisibibi.api.domain.report.entity.AiReportPdfExport;

import java.time.LocalDateTime;

public record AiReportPdfStatusRes(
        Long pdfExportId,
        String pdfType,
        String pdfStatus,
        String notificationStatus,
        boolean downloadAvailable,
        String lastErrorMessage,
        LocalDateTime pdfGeneratedAt,
        LocalDateTime notificationSentAt
) {
    public static AiReportPdfStatusRes notStarted() {
        return new AiReportPdfStatusRes(null, "BASE", "NOT_STARTED", "NOT_SENT", false, null, null, null);
    }

    public static AiReportPdfStatusRes from(AiReportPdfExport export) {
        String lastError = export.getPdfErrorMessage() != null
                ? export.getPdfErrorMessage()
                : export.getNotificationErrorMessage();
        return new AiReportPdfStatusRes(
                export.getId(),
                export.getPdfType().name(),
                export.getPdfStatus().name(),
                export.getNotificationStatus().name(),
                export.isDownloadAvailable(),
                lastError,
                export.getPdfGeneratedAt(),
                export.getNotificationSentAt()
        );
    }
}
