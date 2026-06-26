package com.sisibibi.api.domain.speechreport.dto.response;

import com.sisibibi.api.domain.speechreport.entity.OffTopicAiReview;
import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;
import com.sisibibi.api.domain.speechreport.entity.ViolationSeverity;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

public record SpeechReportDetailRes(
        Long reportId,
        Long speechId,
        Long reportedUserId,
        String reportedUserNickname,
        Long reporterUserId,
        String reporterUserNickname,
        String contentSnapshot,
        SpeechReportReason reason,
        String description,
        SpeechReportStatus status,
        Long reviewedBy,
        String reviewedByNickname,
        LocalDateTime reviewedAt,
        String resolutionNote,
        ViolationSeverity severity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        OffTopicAiReviewRes offTopicAiReview
) {

    public SpeechReportDetailRes(
            Long reportId,
            Long speechId,
            Long reportedUserId,
            Long reporterUserId,
            String contentSnapshot,
            SpeechReportReason reason,
            String description,
            SpeechReportStatus status,
            Long reviewedBy,
            LocalDateTime reviewedAt,
            String resolutionNote,
            ViolationSeverity severity,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(
                reportId,
                speechId,
                reportedUserId,
                null,
                reporterUserId,
                null,
                contentSnapshot,
                reason,
                description,
                status,
                reviewedBy,
                null,
                reviewedAt,
                resolutionNote,
                severity,
                createdAt,
                updatedAt,
                null
        );
    }

    public static SpeechReportDetailRes from(SpeechReport report) {
        return from(report, null, null, null, null);
    }

    public static SpeechReportDetailRes from(
            SpeechReport report,
            @Nullable
            OffTopicAiReview offTopicAiReview
    ) {
        return from(report, null, null, null, offTopicAiReview);
    }

    public static SpeechReportDetailRes from(
            SpeechReport report,
            String reportedUserNickname,
            String reporterUserNickname,
            String reviewedByNickname,
            @Nullable
            OffTopicAiReview offTopicAiReview
    ) {
        return new SpeechReportDetailRes(
                report.getId(),
                report.getSpeechId(),
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
                report.getUpdatedAt(),
                OffTopicAiReviewRes.from(offTopicAiReview)
        );
    }
}
