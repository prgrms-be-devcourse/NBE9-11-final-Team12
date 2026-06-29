package com.sisibibi.api.domain.report.entity;

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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "ai_report_pdf_exports",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_report_pdf_exports_report_user",
                columnNames = {"ai_report_id", "requested_by_user_id"}
        ),
        indexes = {
                @Index(name = "idx_ai_report_pdf_exports_room_user", columnList = "room_id, requested_by_user_id"),
                @Index(name = "idx_ai_report_pdf_exports_pdf_retry", columnList = "pdf_status, pdf_last_attempted_at"),
                @Index(name = "idx_ai_report_pdf_exports_notification_retry", columnList = "notification_status, notification_last_attempted_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiReportPdfExport {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ai_report_id", nullable = false)
    private Long aiReportId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "requested_by_user_id", nullable = false)
    private Long requestedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pdf_status", nullable = false, length = 20)
    private AiReportPdfStatus pdfStatus;

    @Column(name = "pdf_object_key", length = 500)
    private String pdfObjectKey;

    @Column(name = "pdf_generated_at")
    private LocalDateTime pdfGeneratedAt;

    @Column(name = "pdf_error_message", length = ERROR_MESSAGE_MAX_LENGTH)
    private String pdfErrorMessage;

    @Column(name = "pdf_retry_count", nullable = false)
    private int pdfRetryCount;

    @Column(name = "pdf_last_attempted_at")
    private LocalDateTime pdfLastAttemptedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_status", nullable = false, length = 20)
    private AiReportNotificationStatus notificationStatus;

    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    @Column(name = "notification_error_message", length = ERROR_MESSAGE_MAX_LENGTH)
    private String notificationErrorMessage;

    @Column(name = "notification_retry_count", nullable = false)
    private int notificationRetryCount;

    @Column(name = "notification_last_attempted_at")
    private LocalDateTime notificationLastAttemptedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static AiReportPdfExport notStarted(Long aiReportId, Long roomId, Long requestedByUserId) {
        AiReportPdfExport export = new AiReportPdfExport();
        export.aiReportId = aiReportId;
        export.roomId = roomId;
        export.requestedByUserId = requestedByUserId;
        export.pdfStatus = AiReportPdfStatus.NOT_STARTED;
        export.pdfRetryCount = 0;
        export.notificationStatus = AiReportNotificationStatus.NOT_SENT;
        export.notificationRetryCount = 0;
        return export;
    }

    public boolean isDownloadAvailable() {
        return pdfStatus == AiReportPdfStatus.READY && StringUtils.hasText(pdfObjectKey);
    }

    public boolean shouldStartGeneration() {
        return pdfStatus == AiReportPdfStatus.NOT_STARTED || pdfStatus == AiReportPdfStatus.FAILED;
    }

    public void markGenerating(LocalDateTime attemptedAt) {
        this.pdfStatus = AiReportPdfStatus.GENERATING;
        this.pdfLastAttemptedAt = attemptedAt;
        this.pdfErrorMessage = null;
    }

    public void markPdfReady(String objectKey, LocalDateTime generatedAt) {
        this.pdfStatus = AiReportPdfStatus.READY;
        this.pdfObjectKey = objectKey;
        this.pdfGeneratedAt = generatedAt;
        this.pdfErrorMessage = null;
        this.notificationStatus = AiReportNotificationStatus.NOT_SENT;
        this.notificationSentAt = null;
        this.notificationErrorMessage = null;
    }

    public void markPdfFailed(String errorMessage, LocalDateTime attemptedAt) {
        this.pdfStatus = AiReportPdfStatus.FAILED;
        this.pdfRetryCount++;
        this.pdfLastAttemptedAt = attemptedAt;
        this.pdfErrorMessage = truncate(errorMessage);
    }

    public void markNotificationSent(LocalDateTime sentAt) {
        this.notificationStatus = AiReportNotificationStatus.SENT;
        this.notificationSentAt = sentAt;
        this.notificationErrorMessage = null;
    }

    public void markNotificationFailed(String errorMessage, LocalDateTime attemptedAt) {
        this.notificationStatus = AiReportNotificationStatus.FAILED;
        this.notificationRetryCount++;
        this.notificationLastAttemptedAt = attemptedAt;
        this.notificationErrorMessage = truncate(errorMessage);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > ERROR_MESSAGE_MAX_LENGTH
                ? message.substring(0, ERROR_MESSAGE_MAX_LENGTH)
                : message;
    }
}
