package com.sisibibi.api.domain.report.entity;

import com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes;
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

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "common_ground", columnDefinition = "TEXT")
    private String commonGround;

    @Column(name = "ai_opinion", columnDefinition = "TEXT")
    private String aiOpinion;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static AiReport pending(Long roomId) {
        AiReport report = new AiReport();
        report.roomId = roomId;
        report.markPending();
        return report;
    }

    public boolean shouldSkipGeneration() {
        return status == AiReportStatus.PENDING || status == AiReportStatus.COMPLETED;
    }

    public void retry() {
        markPending();
    }

    public void complete(AiReportGenerateRes response) {
        this.status = AiReportStatus.COMPLETED;
        this.coreLine = response.coreLine();
        this.keyIssues = response.keyIssues();
        this.aiSummary = response.aiSummary();
        this.commonGround = response.commonGround();
        this.aiOpinion = response.aiOpinion();
        this.errorMessage = null;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.status = AiReportStatus.FAILED;
        this.errorMessage = truncate(errorMessage);
        this.completedAt = null;
    }

    private void markPending() {
        this.status = AiReportStatus.PENDING;
        this.coreLine = null;
        this.keyIssues = List.of();
        this.aiSummary = null;
        this.commonGround = null;
        this.aiOpinion = null;
        this.errorMessage = null;
        this.requestedAt = LocalDateTime.now();
        this.completedAt = null;
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }

        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
