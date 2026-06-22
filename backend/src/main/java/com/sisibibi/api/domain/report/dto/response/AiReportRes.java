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
        String errorMessage,
        LocalDateTime requestedAt,
        LocalDateTime completedAt
) {

    public static AiReportRes from(AiReport report) {
        return new AiReportRes(
                report.getId(),
                report.getRoomId(),
                report.getStatus().name(),
                report.getCoreLine(),
                report.getKeyIssues() == null ? List.of() : report.getKeyIssues(),
                report.getAiSummary(),
                report.getCommonGround(),
                report.getAiOpinion(),
                report.getErrorMessage(),
                report.getRequestedAt(),
                report.getCompletedAt()
        );
    }
}
