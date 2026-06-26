package com.sisibibi.api.domain.speechreport.dto.response;

import com.sisibibi.api.domain.speechreport.entity.OffTopicAiReview;
import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

public record SpeechReportSummaryRes(
        Long reportId,
        Long speechId,
        Long reportedUserId,
        String reportedUserNickname,
        Long reporterUserId,
        String reporterUserNickname,
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
                null,
                reporterUserId,
                null,
                reason,
                status,
                createdAt,
                null
        );
    }

    public static SpeechReportSummaryRes from(SpeechReport report) {
        return from(report, null, null, null);
    }

    public static SpeechReportSummaryRes from(
            SpeechReport report,
            @Nullable
            OffTopicAiReview offTopicAiReview
    ) {
        return from(report, null, null, offTopicAiReview);
    }

    public static SpeechReportSummaryRes from(
            SpeechReport report,
            String reportedUserNickname,
            String reporterUserNickname,
            @Nullable
            OffTopicAiReview offTopicAiReview
    ) {
        return new SpeechReportSummaryRes(
                report.getId(),
                report.getSpeechId(),
                report.getReportedUserId(),
                reportedUserNickname,
                report.getReporterUserId(),
                reporterUserNickname,
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt(),
                OffTopicAiReviewRes.from(offTopicAiReview)
        );
    }
}
