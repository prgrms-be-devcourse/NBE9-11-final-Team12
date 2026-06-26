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
}
