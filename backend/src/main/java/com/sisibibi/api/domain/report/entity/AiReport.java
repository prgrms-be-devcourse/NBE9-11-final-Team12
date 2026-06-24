package com.sisibibi.api.domain.report.entity;

import com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes;
import com.sisibibi.api.domain.report.client.dto.AiReportCustomReportPayload;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "ai_reports",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_reports_room_id", columnNames = "room_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiReportStatus status;

    @Column(name = "core_line", columnDefinition = "TEXT")
    private String coreLine;

    @Convert(converter = KeyIssuesConverter.class)
    @Column(name = "key_issues", columnDefinition = "TEXT")
    private List<String> keyIssues = List.of();

    @Convert(converter = CustomPromptsConverter.class)
    @Column(name = "custom_prompts", columnDefinition = "TEXT")
    private List<AiReportCustomPrompt> customPrompts = List.of();

    @Convert(converter = CustomReportsConverter.class)
    @Column(name = "custom_reports", columnDefinition = "TEXT")
    private List<AiReportCustomReport> customReports = List.of();

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "common_ground", columnDefinition = "TEXT")
    private String commonGround;

    @Column(name = "ai_opinion", columnDefinition = "TEXT")
    private String aiOpinion;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "publish_retry_count", nullable = false)
    private int publishRetryCount;

    @Column(name = "generation_retry_count", nullable = false)
    private int generationRetryCount;

    @Column(name = "last_error_code", length = 100)
    private String lastErrorCode;

    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "processing_locked_until")
    private LocalDateTime processingLockedUntil;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "queued_at")
    private LocalDateTime queuedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static AiReport pending(Long roomId) {
        return requested(roomId);
    }

    public static AiReport pending(Long roomId, List<AiReportCustomPrompt> customPrompts) {
        return requested(roomId, customPrompts);
    }

    public static AiReport requested(Long roomId) {
        return requested(roomId, List.of());
    }

    public static AiReport requested(Long roomId, List<AiReportCustomPrompt> customPrompts) {
        AiReport report = new AiReport();
        report.roomId = roomId;
        report.markRequested(customPrompts);
        return report;
    }

    public boolean shouldSkipGeneration() {
        return isGenerationInProgress() || status == AiReportStatus.COMPLETED;
    }

    public boolean isGenerationInProgress() {
        return status == AiReportStatus.REQUESTED
                || status == AiReportStatus.QUEUED
                || status == AiReportStatus.PROCESSING
                || status == AiReportStatus.PENDING;
    }

    public void retry() {
        retry(List.of());
    }

    public void retry(List<AiReportCustomPrompt> customPrompts) {
        markRequested(customPrompts);
    }

    public void complete(AiReportGenerateRes response) {
        this.status = AiReportStatus.COMPLETED;
        this.coreLine = response.coreLine();
        this.keyIssues = response.keyIssues();
        this.aiSummary = response.aiSummary();
        this.commonGround = response.commonGround();
        this.aiOpinion = response.aiOpinion();
        this.customReports = toCustomReports(this.customPrompts, response.customReports());
        this.errorMessage = null;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
        this.failedAt = null;
        this.completedAt = LocalDateTime.now();
    }

    public void appendCustomReports(
            Long userId,
            List<AiReportCustomPrompt> customPrompts,
            List<AiReportCustomReportPayload> customReportPayloads
    ) {
        this.customPrompts = mergePrompts(this.customPrompts, customPrompts);
        this.customReports = mergeReports(this.customReports, toCustomReports(userId, customPrompts, customReportPayloads));
        this.errorMessage = null;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
    }

    public void appendCustomReports(
            List<AiReportCustomPrompt> customPrompts,
            List<AiReportCustomReportPayload> customReportPayloads
    ) {
        appendCustomReports(null, customPrompts, customReportPayloads);
    }

    public void fail(String errorMessage) {
        this.status = AiReportStatus.GENERATION_FAILED;
        this.errorMessage = truncate(errorMessage);
        this.lastErrorCode = null;
        this.lastErrorMessage = truncate(errorMessage);
        this.completedAt = null;
        this.failedAt = LocalDateTime.now();
    }

    private void markRequested(List<AiReportCustomPrompt> customPrompts) {
        this.status = AiReportStatus.REQUESTED;
        this.coreLine = null;
        this.keyIssues = List.of();
        this.customPrompts = customPrompts == null ? List.of() : List.copyOf(customPrompts);
        this.customReports = List.of();
        this.aiSummary = null;
        this.commonGround = null;
        this.aiOpinion = null;
        this.errorMessage = null;
        this.lastErrorCode = null;
        this.lastErrorMessage = null;
        this.processingStartedAt = null;
        this.processingLockedUntil = null;
        this.requestedAt = LocalDateTime.now();
        this.queuedAt = null;
        this.completedAt = null;
        this.failedAt = null;
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }

        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private List<AiReportCustomReport> toCustomReports(
            List<AiReportCustomPrompt> prompts,
            List<AiReportCustomReportPayload> reports
    ) {
        if (reports == null || reports.isEmpty()) {
            return List.of();
        }

        return java.util.stream.IntStream.range(0, reports.size())
                .mapToObj(index -> {
                    AiReportCustomPrompt prompt = prompts != null && prompts.size() > index
                            ? prompts.get(index)
                            : new AiReportCustomPrompt("custom " + (index + 1), "");
                    AiReportCustomReportPayload report = reports.get(index);
                    return new AiReportCustomReport(
                            prompt.userId(),
                            prompt.label(),
                            prompt.prompt(),
                            report.label(),
                            report.content()
                    );
                })
                .toList();
    }

    private List<AiReportCustomReport> toCustomReports(
            Long userId,
            List<AiReportCustomPrompt> prompts,
            List<AiReportCustomReportPayload> reports
    ) {
        if (reports == null || reports.isEmpty()) {
            return List.of();
        }

        return java.util.stream.IntStream.range(0, reports.size())
                .mapToObj(index -> {
                    AiReportCustomPrompt prompt = prompts != null && prompts.size() > index
                            ? prompts.get(index)
                            : new AiReportCustomPrompt(userId, "custom " + (index + 1), "");
                    AiReportCustomReportPayload report = reports.get(index);
                    return new AiReportCustomReport(
                            prompt.userId() == null ? userId : prompt.userId(),
                            prompt.label(),
                            prompt.prompt(),
                            report.label(),
                            report.content()
                    );
                })
                .toList();
    }

    private List<AiReportCustomPrompt> mergePrompts(
            List<AiReportCustomPrompt> existingPrompts,
            List<AiReportCustomPrompt> newPrompts
    ) {
        if (newPrompts == null || newPrompts.isEmpty()) {
            return existingPrompts == null ? List.of() : List.copyOf(existingPrompts);
        }

        java.util.ArrayList<AiReportCustomPrompt> merged = new java.util.ArrayList<>(
                existingPrompts == null ? List.of() : existingPrompts
        );
        merged.addAll(newPrompts);
        return List.copyOf(merged);
    }

    private List<AiReportCustomReport> mergeReports(
            List<AiReportCustomReport> existingReports,
            List<AiReportCustomReport> newReports
    ) {
        if (newReports == null || newReports.isEmpty()) {
            return existingReports == null ? List.of() : List.copyOf(existingReports);
        }

        java.util.ArrayList<AiReportCustomReport> merged = new java.util.ArrayList<>(
                existingReports == null ? List.of() : existingReports
        );
        merged.addAll(newReports);
        return List.copyOf(merged);
    }
}
