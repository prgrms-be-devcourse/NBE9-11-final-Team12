package com.sisibibi.api.domain.chatreport.dto.response;

import com.sisibibi.api.domain.chatreport.entity.ChatReport;
import com.sisibibi.api.domain.chatreport.entity.ChatReportReason;
import com.sisibibi.api.domain.chatreport.entity.ChatReportStatus;
import java.time.LocalDateTime;

public record ChatReportCreateRes(
        Long reportId,
        Long roomId,
        Long messageId,
        ChatReportReason reason,
        ChatReportStatus status,
        LocalDateTime createdAt
) {

    public static ChatReportCreateRes from(ChatReport report) {
        return new ChatReportCreateRes(
                report.getId(),
                report.getRoomId(),
                report.getMessageId(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
