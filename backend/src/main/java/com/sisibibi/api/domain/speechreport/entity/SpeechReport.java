package com.sisibibi.api.domain.speechreport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "speech_reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_speech_reports_speech_reporter",
                columnNames = {"speech_id", "reporter_user_id"}
        ),
        indexes = @Index(
                name = "idx_speech_reports_status_created_at",
                columnList = "status, created_at"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpeechReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "speech_id", nullable = false)
    private Long speechId;

    @Column(name = "reported_user_id", nullable = false)
    private Long reportedUserId;

    @Column(name = "reporter_user_id", nullable = false)
    private Long reporterUserId;

    @Column(name = "content_snapshot", nullable = false, columnDefinition = "TEXT")
    private String contentSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SpeechReportReason reason;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpeechReportStatus status;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private SpeechReport(
            Long speechId,
            Long reportedUserId,
            Long reporterUserId,
            String contentSnapshot,
            SpeechReportReason reason,
            String description
    ) {
        this.speechId = speechId;
        this.reportedUserId = reportedUserId;
        this.reporterUserId = reporterUserId;
        this.contentSnapshot = contentSnapshot;
        this.reason = reason;
        this.description = normalizeDescription(description);
        this.status = SpeechReportStatus.PENDING;
    }

    public static SpeechReport create(
            Long speechId,
            Long reportedUserId,
            Long reporterUserId,
            String contentSnapshot,
            SpeechReportReason reason,
            String description
    ) {
        return new SpeechReport(
                speechId,
                reportedUserId,
                reporterUserId,
                contentSnapshot,
                reason,
                description
        );
    }

    public void review(
            SpeechReportReviewAction action,
            Long reviewerUserId,
            String resolutionNote,
            LocalDateTime now
    ) {
        switch (action) {
            case START_REVIEW -> startReview(reviewerUserId);
            case RESOLVE -> completeReview(
                    SpeechReportStatus.RESOLVED,
                    reviewerUserId,
                    resolutionNote,
                    now
            );
            case REJECT -> completeReview(
                    SpeechReportStatus.REJECTED,
                    reviewerUserId,
                    resolutionNote,
                    now
            );
        }
    }

    private void startReview(Long reviewerUserId) {
        if (status != SpeechReportStatus.PENDING) {
            throw new CustomException(ErrorCode.SPEECH_REPORT_INVALID_STATUS_TRANSITION);
        }

        status = SpeechReportStatus.REVIEWING;
        reviewedBy = reviewerUserId;
    }

    private void completeReview(
            SpeechReportStatus targetStatus,
            Long reviewerUserId,
            String resolutionNote,
            LocalDateTime now
    ) {
        if (status != SpeechReportStatus.REVIEWING) {
            throw new CustomException(ErrorCode.SPEECH_REPORT_INVALID_STATUS_TRANSITION);
        }
        if (resolutionNote == null || resolutionNote.isBlank()) {
            throw new CustomException(ErrorCode.SPEECH_REPORT_RESOLUTION_NOTE_REQUIRED);
        }

        status = targetStatus;
        reviewedBy = reviewerUserId;
        reviewedAt = now;
        this.resolutionNote = resolutionNote.trim();
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
