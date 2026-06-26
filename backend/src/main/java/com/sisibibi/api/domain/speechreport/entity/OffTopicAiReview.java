package com.sisibibi.api.domain.speechreport.entity;

import com.sisibibi.api.domain.speechreport.service.OffTopicAiReviewResult;
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

import java.time.LocalDateTime;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "off_topic_ai_reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_off_topic_ai_reviews_speech_id",
                columnNames = "speech_id"
        ),
        indexes = @Index(
                name = "idx_off_topic_ai_reviews_status_created_at",
                columnList = "status, created_at"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OffTopicAiReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "speech_id", nullable = false)
    private Long speechId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "content_snapshot", nullable = false, columnDefinition = "TEXT")
    private String contentSnapshot;

    @Column(name = "report_count", nullable = false)
    private int reportCount;

    @Column(nullable = false)
    private int threshold;

    @Column(name = "participant_count", nullable = false)
    private int participantCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OffTopicAiReviewStatus status;

    @Column(name = "is_off_topic")
    private Boolean offTopic;

    @Column
    private Double confidence;

    @Column(length = 500)
    private String reason;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private OffTopicAiReview(
            Long speechId,
            Long roomId,
            String contentSnapshot,
            int reportCount,
            int threshold,
            int participantCount
    ) {
        this.speechId = speechId;
        this.roomId = roomId;
        this.contentSnapshot = contentSnapshot;
        this.reportCount = reportCount;
        this.threshold = threshold;
        this.participantCount = participantCount;
        this.status = OffTopicAiReviewStatus.PENDING;
    }

    public static OffTopicAiReview pending(
            Long speechId,
            Long roomId,
            String contentSnapshot,
            int reportCount,
            int threshold,
            int participantCount
    ) {
        return new OffTopicAiReview(
                speechId,
                roomId,
                contentSnapshot,
                reportCount,
                threshold,
                participantCount
        );
    }

    public void complete(OffTopicAiReviewResult result, LocalDateTime completedAt) {
        this.status = OffTopicAiReviewStatus.COMPLETED;
        this.offTopic = result.offTopic();
        this.reason = normalize(result.reason());
        this.confidence = result.confidence();
        this.errorMessage = null;
        this.completedAt = completedAt;
    }

    public void fail(String errorMessage) {
        this.status = OffTopicAiReviewStatus.FAILED;
        this.errorMessage = normalize(errorMessage);
    }

    public boolean isOffTopic() {
        return Boolean.TRUE.equals(offTopic);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 500) {
            return trimmed;
        }
        return trimmed.substring(0, 500);
    }
}
