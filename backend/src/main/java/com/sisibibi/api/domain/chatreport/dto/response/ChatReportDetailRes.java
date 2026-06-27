package com.sisibibi.api.domain.chatreport.dto.response;

import com.sisibibi.api.domain.chatreport.entity.ChatReport;
import com.sisibibi.api.domain.chatreport.entity.ChatReportReason;
import com.sisibibi.api.domain.chatreport.entity.ChatReportSeverity;
import com.sisibibi.api.domain.chatreport.entity.ChatReportStatus;

import java.time.LocalDateTime;

public record ChatReportDetailRes(
        Long reportId,
        Long roomId,
        Long messageId,
        Long reportedUserId,
        String reportedUserNickname,
        Long reporterUserId,
        String reporterUserNickname,
        String contentSnapshot,
        ChatReportReason reason,
        String description,
        ChatReportStatus status,
        Long reviewedBy,
        String reviewedByNickname,
        LocalDateTime reviewedAt,
        String resolutionNote,
        ChatReportSeverity severity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ChatReportDetailRes from(
            ChatReport report,
            String reportedUserNickname,
            String reporterUserNickname,
            String reviewedByNickname
    ) {
        return new ChatReportDetailRes(
                report.getId(),
                report.getRoomId(),
                report.getMessageId(),
                report.getReportedUserId(),
                reportedUserNickname,
                report.getReporterUserId(),
                reporterUserNickname,
                report.getContentSnapshot(),
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getReviewedBy(),
                reviewedByNickname,
                report.getReviewedAt(),
                report.getResolutionNote(),
                report.getSeverity(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
