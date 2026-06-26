package com.sisibibi.api.domain.chatreport.entity;

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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "chat_reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_reports_message_reporter",
                columnNames = {"message_id", "reporter_user_id"}
        ),
        indexes = @Index(
                name = "idx_chat_reports_status_created_at",
                columnList = "status, created_at"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "reported_user_id", nullable = false)
    private Long reportedUserId;

    @Column(name = "reporter_user_id", nullable = false)
    private Long reporterUserId;

    @Column(name = "content_snapshot", nullable = false, columnDefinition = "TEXT")
    private String contentSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChatReportReason reason;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatReportStatus status;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ChatReportSeverity severity;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private ChatReport(
            Long roomId,
            Long messageId,
            Long reportedUserId,
            Long reporterUserId,
            String contentSnapshot,
            ChatReportReason reason,
            String description
    ) {
        this.roomId = roomId;
        this.messageId = messageId;
        this.reportedUserId = reportedUserId;
        this.reporterUserId = reporterUserId;
        this.contentSnapshot = contentSnapshot;
        this.reason = reason;
        this.description = normalizeDescription(description);
        this.status = ChatReportStatus.PENDING;
    }

    public static ChatReport create(
            Long roomId,
            Long messageId,
            Long reportedUserId,
            Long reporterUserId,
            String contentSnapshot,
            ChatReportReason reason,
            String description
    ) {
        return new ChatReport(
                roomId,
                messageId,
                reportedUserId,
                reporterUserId,
                contentSnapshot,
                reason,
                description
        );
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    public void review(
            ChatReportReviewAction action,
            Long reviewerUserId,
            String resolutionNote,
            ChatReportSeverity severity,
            LocalDateTime now
    ) {
        switch (action) {
            case START_REVIEW -> startReview(reviewerUserId, severity);
            case RESOLVE -> resolve(
                    reviewerUserId,
                    resolutionNote,
                    severity,
                    now
            );
            case REJECT -> reject(
                    reviewerUserId,
                    resolutionNote,
                    severity,
                    now
            );
        }
    }

    private void startReview(Long reviewerUserId, ChatReportSeverity severity) {
        if (status != ChatReportStatus.PENDING) {
            throw new CustomException(ErrorCode.CHAT_REPORT_INVALID_STATUS_TRANSITION);
        }
        validateSeverityNotAllowed(severity);

        status = ChatReportStatus.REVIEWING;
        reviewedBy = reviewerUserId;
    }

    private void resolve(
            Long reviewerUserId,
            String resolutionNote,
            ChatReportSeverity severity,
            LocalDateTime now
    ) {
        validateReviewing();
        if (severity == null) {
            throw new CustomException(ErrorCode.CHAT_REPORT_SEVERITY_REQUIRED);
        }
        completeReview(
                ChatReportStatus.RESOLVED,
                reviewerUserId,
                resolutionNote,
                severity,
                now
        );
    }

    private void reject(
            Long reviewerUserId,
            String resolutionNote,
            ChatReportSeverity severity,
            LocalDateTime now
    ) {
        validateReviewing();
        validateSeverityNotAllowed(severity);
        completeReview(
                ChatReportStatus.REJECTED,
                reviewerUserId,
                resolutionNote,
                null,
                now
        );
    }

    private void completeReview(
            ChatReportStatus targetStatus,
            Long reviewerUserId,
            String resolutionNote,
            ChatReportSeverity severity,
            LocalDateTime now
    ) {
        if (resolutionNote == null || resolutionNote.isBlank()) {
            throw new CustomException(ErrorCode.CHAT_REPORT_RESOLUTION_NOTE_REQUIRED);
        }

        String normalizedResolutionNote = resolutionNote.trim();
        if (normalizedResolutionNote.length() > 500) {
            throw new CustomException(ErrorCode.CHAT_REPORT_RESOLUTION_NOTE_TOO_LONG);
        }

        status = targetStatus;
        reviewedBy = reviewerUserId;
        reviewedAt = now;
        this.resolutionNote = normalizedResolutionNote;
        this.severity = severity;
    }

    private void validateReviewing() {
        if (status != ChatReportStatus.REVIEWING) {
            throw new CustomException(ErrorCode.CHAT_REPORT_INVALID_STATUS_TRANSITION);
        }
    }

    private void validateSeverityNotAllowed(ChatReportSeverity severity) {
        if (severity != null) {
            throw new CustomException(ErrorCode.CHAT_REPORT_SEVERITY_NOT_ALLOWED);
        }
    }
}
