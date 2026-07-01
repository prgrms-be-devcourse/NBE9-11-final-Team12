package com.sisibibi.api.domain.report.dto.response;

import com.sisibibi.api.domain.report.entity.AiReport;

import java.time.LocalDateTime;
import java.util.List;

public record AiReportRes(
        Long reportId,
        Long roomId,
        String status,
        String coreLine,
        List<String> keyIssues,
        String aiSummary,
        String commonGround,
        String aiOpinion,
        List<CustomReportRes> customReports,
        String errorMessage,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        AiReportPdfStatusRes pdf
) {

    public AiReportRes(
            Long reportId,
            Long roomId,
            String status,
            String coreLine,
            List<String> keyIssues,
            String aiSummary,
            String commonGround,
            String aiOpinion,
            String errorMessage,
            LocalDateTime requestedAt,
            LocalDateTime completedAt
    ) {
        this(
                reportId,
                roomId,
                status,
                coreLine,
                keyIssues,
                aiSummary,
                commonGround,
                aiOpinion,
                List.of(),
                errorMessage,
                requestedAt,
                completedAt,
                AiReportPdfStatusRes.notStarted()
        );
    }

    public static AiReportRes from(AiReport report) {
        return from(report, null);
    }

    public static AiReportRes from(AiReport report, Long viewerUserId) {
        return from(report, viewerUserId, null);
    }

    public static AiReportRes from(AiReport report, Long viewerUserId, com.sisibibi.api.domain.report.entity.AiReportPdfExport export) {
        return new AiReportRes(
                report.getId(),
                report.getRoomId(),
                report.getStatus().name(),
                report.getCoreLine(),
                report.getKeyIssues() == null ? List.of() : report.getKeyIssues(),
                report.getAiSummary(),
                report.getCommonGround(),
                report.getAiOpinion(),
                report.getCustomReports() == null
                        ? List.of()
                        : report.getCustomReports().stream()
                        .filter(customReport -> customReport.isVisibleTo(viewerUserId))
                        .map(customReport -> new CustomReportRes(
                                customReport.requestLabel(),
                                customReport.prompt(),
                                customReport.resultLabel(),
                                customReport.content()
                        ))
                        .toList(),
                report.getLastErrorMessage() == null ? report.getErrorMessage() : report.getLastErrorMessage(),
                report.getRequestedAt(),
                report.getCompletedAt(),
                export == null ? AiReportPdfStatusRes.notStarted() : AiReportPdfStatusRes.from(export)
        );
    }

    public AiReportRes withPdf(AiReportPdfStatusRes newPdf) {
        return new AiReportRes(
                reportId, roomId, status, coreLine, keyIssues,
                aiSummary, commonGround, aiOpinion, customReports,
                errorMessage, requestedAt, completedAt, newPdf
        );
    }

    public record CustomReportRes(
            String requestLabel,
            String prompt,
            String label,
            String content
    ) {
    }
}
