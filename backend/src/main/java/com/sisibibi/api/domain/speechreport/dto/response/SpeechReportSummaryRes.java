package com.sisibibi.api.domain.speechreport.dto.response;

import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.OffTopicAiReview;
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
        LocalDateTime createdAt,
        OffTopicAiReviewRes offTopicAiReview
) {

    public SpeechReportSummaryRes(
            Long reportId,
            Long speechId,
            Long reportedUserId,
            Long reporterUserId,
            SpeechReportReason reason,
            SpeechReportStatus status,
            LocalDateTime createdAt
    ) {
        this(
                reportId,
                speechId,
                reportedUserId,
                reporterUserId,
                reason,
                status,
                createdAt,
                null
        );
    }

    public static SpeechReportSummaryRes from(SpeechReport report) {
        return from(report, null);
    }

    public static SpeechReportSummaryRes from(
            SpeechReport report,
            OffTopicAiReview offTopicAiReview
    ) {
        return new SpeechReportSummaryRes(
                report.getId(),
                report.getSpeechId(),
                report.getReportedUserId(),
                report.getReporterUserId(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt(),
                OffTopicAiReviewRes.from(offTopicAiReview)
        );
    }
}
