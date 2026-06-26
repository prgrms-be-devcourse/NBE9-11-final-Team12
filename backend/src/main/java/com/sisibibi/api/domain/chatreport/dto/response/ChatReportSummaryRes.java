package com.sisibibi.api.domain.chatreport.dto.response;

import com.sisibibi.api.domain.chatreport.entity.ChatReport;
import com.sisibibi.api.domain.chatreport.entity.ChatReportReason;
import com.sisibibi.api.domain.chatreport.entity.ChatReportStatus;

import java.time.LocalDateTime;

public record ChatReportSummaryRes(
        Long reportId,
        Long roomId,
        Long messageId,
        Long reportedUserId,
        String reportedUserNickname,
        Long reporterUserId,
        String reporterUserNickname,
        ChatReportReason reason,
        ChatReportStatus status,
        LocalDateTime createdAt
) {

    public static ChatReportSummaryRes from(
            ChatReport report,
            String reportedUserNickname,
            String reporterUserNickname
    ) {
        return new ChatReportSummaryRes(
                report.getId(),
                report.getRoomId(),
                report.getMessageId(),
                report.getReportedUserId(),
                reportedUserNickname,
                report.getReporterUserId(),
                reporterUserNickname,
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
