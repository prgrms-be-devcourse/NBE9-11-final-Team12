package com.sisibibi.api.domain.speech.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "stage_summaries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stage_summaries_room_id",
                columnNames = "room_id"
        ),
        indexes = @Index(
                name = "idx_stage_summaries_status_retry",
                columnList = "status, retry_count, created_at"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StageSummary {

    private static final int ERROR_MESSAGE_MAX_LENGTH = 2_000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StageSummaryStatus status;

    @Column(name = "moderator_summary", columnDefinition = "TEXT")
    private String moderatorSummary;

    @Convert(converter = KeyPointsConverter.class)
    @Column(name = "key_points", columnDefinition = "TEXT")
    private List<String> keyPoints = List.of();

    @Column(name = "speech_count", nullable = false)
    private int speechCount;

    @Column(name = "completed_speaker_count", nullable = false)
    private int completedSpeakerCount;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_attempted_at")
    private LocalDateTime lastAttemptedAt;

    @Column(name = "error_message", length = ERROR_MESSAGE_MAX_LENGTH)
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static StageSummary pending(
            Long roomId,
            LocalDateTime triggeredAt,
            int completedSpeakerCount
    ) {
        StageSummary summary = new StageSummary();
        summary.roomId = roomId;
        summary.status = StageSummaryStatus.PENDING;
        summary.completedSpeakerCount = completedSpeakerCount;
        summary.triggeredAt = triggeredAt;
        summary.retryCount = 0;
        summary.keyPoints = List.of();
        return summary;
    }

    public boolean shouldSkipGeneration() {
        return status == StageSummaryStatus.PENDING || status == StageSummaryStatus.COMPLETED;
    }

    public boolean canRetry(int maxAttempts) {
        return status == StageSummaryStatus.FAILED && retryCount < maxAttempts;
    }

    public void markPendingForRetry() {
        if (status != StageSummaryStatus.FAILED) {
            throw new IllegalStateException("Only failed stage summaries can be retried.");
        }

        this.status = StageSummaryStatus.PENDING;
        this.errorMessage = null;
    }

    public void markAttemptStarted(LocalDateTime attemptedAt) {
        this.lastAttemptedAt = attemptedAt;
    }

    public void complete(
            String moderatorSummary,
            List<String> keyPoints,
            int speechCount,
            LocalDateTime completedAt
    ) {
        this.status = StageSummaryStatus.COMPLETED;
        this.moderatorSummary = moderatorSummary;
        this.keyPoints = keyPoints == null ? List.of() : List.copyOf(keyPoints);
        this.speechCount = speechCount;
        this.completedAt = completedAt;
        this.errorMessage = null;
    }

    public void fail(String errorMessage) {
        this.status = StageSummaryStatus.FAILED;
        this.retryCount++;
        this.errorMessage = truncate(errorMessage);
        this.completedAt = null;
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
