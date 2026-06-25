package com.sisibibi.api.domain.speech.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "ai_counter_issues",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ai_counter_issues_room_trigger_queue",
                columnNames = {"room_id", "trigger_queue_id"}
        ),
        indexes = @Index(
                name = "idx_ai_counter_issues_room_created",
                columnList = "room_id, created_at"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiCounterIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "trigger_queue_id", nullable = false)
    private Long triggerQueueId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_stance", nullable = false, length = 10)
    private SpeechStance targetStance;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiCounterIssueStatus status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public static AiCounterIssue pending(
            Long roomId,
            Long triggerQueueId,
            SpeechStance targetStance
    ) {
        AiCounterIssue issue = new AiCounterIssue();
        issue.roomId = roomId;
        issue.triggerQueueId = triggerQueueId;
        issue.targetStance = targetStance;
        issue.status = AiCounterIssueStatus.PENDING;
        return issue;
    }

    public void complete(String content, LocalDateTime completedAt) {
        this.content = content;
        this.status = AiCounterIssueStatus.COMPLETED;
        this.completedAt = completedAt;
        this.errorMessage = null;
    }

    public void fail(String errorMessage) {
        this.status = AiCounterIssueStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}