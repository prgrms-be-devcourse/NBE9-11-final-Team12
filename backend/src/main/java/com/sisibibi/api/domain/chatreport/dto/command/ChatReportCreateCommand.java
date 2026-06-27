package com.sisibibi.api.domain.chatreport.dto.command;

import com.sisibibi.api.domain.chatreport.entity.ChatReportReason;

public record ChatReportCreateCommand(
        Long roomId,
        Long messageId,
        Long reportedUserId,
        Long reporterUserId,
        String contentSnapshot,
        ChatReportReason reason,
        String description
) {
}
