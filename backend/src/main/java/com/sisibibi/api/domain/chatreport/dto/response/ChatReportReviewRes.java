package com.sisibibi.api.domain.chatreport.dto.response;

import com.sisibibi.api.domain.chatreport.entity.ChatReport;
import com.sisibibi.api.domain.chatreport.entity.ChatReportSeverity;
import com.sisibibi.api.domain.chatreport.entity.ChatReportStatus;

import java.time.LocalDateTime;

public record ChatReportReviewRes(
        Long reportId,
        ChatReportStatus status,
        Long reviewedBy,
        String reviewedByNickname,
        LocalDateTime reviewedAt,
        String resolutionNote,
        ChatReportSeverity severity
) {

    public static ChatReportReviewRes from(ChatReport report, String reviewedByNickname) {
        return new ChatReportReviewRes(
                report.getId(),
                report.getStatus(),
                report.getReviewedBy(),
                reviewedByNickname,
                report.getReviewedAt(),
                report.getResolutionNote(),
                report.getSeverity()
        );
    }
}
