package com.sisibibi.api.domain.speechreport.dto.response;

import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;

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
