package com.sisibibi.api.domain.speech.report.dto.response;

import com.sisibibi.api.domain.speech.report.entity.SpeechReport;
import com.sisibibi.api.domain.speech.report.entity.SpeechReportReason;
import com.sisibibi.api.domain.speech.report.entity.SpeechReportStatus;

import java.time.LocalDateTime;

public record SpeechReportCreateRes(
        Long reportId,
        Long speechId,
        SpeechReportReason reason,
        SpeechReportStatus status,
        LocalDateTime createdAt
) {

    public static SpeechReportCreateRes from(SpeechReport report) {
        return new SpeechReportCreateRes(
                report.getId(),
                report.getSpeechId(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
