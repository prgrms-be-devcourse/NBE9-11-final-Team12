package com.sisibibi.api.domain.speechreport.dto.response;

import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;
import com.sisibibi.api.domain.speechreport.entity.ViolationSeverity;

import java.time.LocalDateTime;

public record SpeechReportReviewRes(
        Long reportId,
        SpeechReportStatus status,
        Long reviewedBy,
        String reviewedByNickname,
        LocalDateTime reviewedAt,
        String resolutionNote,
        ViolationSeverity severity
) {

    public static SpeechReportReviewRes from(SpeechReport report, String reviewedByNickname) {
        return new SpeechReportReviewRes(
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
