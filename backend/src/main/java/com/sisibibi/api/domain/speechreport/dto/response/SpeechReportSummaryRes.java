package com.sisibibi.api.domain.speechreport.dto.response;

import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;

import java.time.LocalDateTime;

public record SpeechReportSummaryRes(
        Long reportId,
        Long speechId,
        Long reportedUserId,
        Long reporterUserId,
        SpeechReportReason reason,
        SpeechReportStatus status,
        LocalDateTime createdAt
) {

    public static SpeechReportSummaryRes from(SpeechReport report) {
        return new SpeechReportSummaryRes(
                report.getId(),
                report.getSpeechId(),
                report.getReportedUserId(),
                report.getReporterUserId(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
