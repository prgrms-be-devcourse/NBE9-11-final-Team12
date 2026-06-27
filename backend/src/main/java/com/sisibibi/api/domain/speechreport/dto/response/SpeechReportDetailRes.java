package com.sisibibi.api.domain.speechreport.dto.response;

import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.OffTopicAiReview;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;
import com.sisibibi.api.domain.speechreport.entity.ViolationSeverity;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

public record SpeechReportDetailRes(
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
                reporterUserId,
                contentSnapshot,
                reason,
                description,
                status,
                reviewedBy,
                reviewedAt,
                resolutionNote,
                severity,
                createdAt,
                updatedAt,
                null
        );
    }

    public static SpeechReportDetailRes from(SpeechReport report) {
        return from(report, null);
    }

    public static SpeechReportDetailRes from(
            SpeechReport report,
            @Nullable
            OffTopicAiReview offTopicAiReview
    ) {
        return new SpeechReportDetailRes(
                report.getId(),
                report.getSpeechId(),
                report.getReportedUserId(),
                report.getReporterUserId(),
                report.getContentSnapshot(),
                report.getReason(),
                report.getDescription(),
                report.getStatus(),
                report.getReviewedBy(),
                report.getReviewedAt(),
                report.getResolutionNote(),
                report.getSeverity(),
                report.getCreatedAt(),
                report.getUpdatedAt(),
                OffTopicAiReviewRes.from(offTopicAiReview)
        );
    }
}
