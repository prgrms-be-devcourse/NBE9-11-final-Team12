package com.sisibibi.api.domain.speech.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "speaking_queue",
        indexes = {
                @Index(
                        name = "idx_speaking_queue_room_status_order",
                        columnList = "room_id, status, queue_order"
                ),
                @Index(
                        name = "idx_speaking_queue_room_user_status",
                        columnList = "room_id, user_id, status"
                ),
                @Index(
                        name = "idx_speaking_queue_status_expires_at",
                        columnList = "status, expires_at"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_speaking_queue_room_order",
                        columnNames = {"room_id", "queue_order"}
                ),
                @UniqueConstraint(
                        name = "uk_speaking_queue_room_user_active",
                        columnNames = {"room_id", "user_id", "active_request"}
                )
        }
)
public class SpeakingQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "queue_order")
    private Integer queueOrder;

    @Column(name = "active_request")
    private Boolean activeRequest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpeakingQueueStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    private SpeakingQueue(
            Long roomId,
            Long userId,
            LocalDateTime requestedAt
    ) {
        this.roomId = roomId;
        this.userId = userId;
        this.activeRequest = true;
        this.status = SpeakingQueueStatus.WAITING;
        this.requestedAt = requestedAt;
    }

    public static SpeakingQueue waiting(
            Long roomId,
            Long userId,
            LocalDateTime requestedAt
    ) {
        return new SpeakingQueue(roomId, userId, requestedAt);
    }

    public static SpeakingQueue waiting(
            Long roomId,
            Long userId,
            int queueOrder,
            LocalDateTime requestedAt
    ) {
        SpeakingQueue speakingQueue = new SpeakingQueue(roomId, userId, requestedAt);
        speakingQueue.queueOrder = queueOrder;
        return speakingQueue;
    }

    public void assignQueueOrderFromId() {
        if (id == null) {
            throw new IllegalStateException("Speaking queue must be persisted first.");
        }

        this.queueOrder = Math.toIntExact(id);
    }

    public void assign(LocalDateTime assignedAt, LocalDateTime expiresAt) {
        if (status != SpeakingQueueStatus.WAITING) {
            throw new IllegalStateException(
                    "Only waiting speaking requests can be assigned."
            );
        }
        if (assignedAt == null || expiresAt == null || !expiresAt.isAfter(assignedAt)) {
            throw new IllegalArgumentException(
                    "Speaking expiration must be after assignment."
            );
        }

        this.status = SpeakingQueueStatus.ASSIGNED;
        this.assignedAt = assignedAt;
        this.expiresAt = expiresAt;
    }

    public void cancel(LocalDateTime canceledAt) {
        if (status != SpeakingQueueStatus.WAITING) {
            throw new IllegalStateException(
                    "Only waiting speaking requests can be canceled."
            );
        }

        this.status = SpeakingQueueStatus.CANCELED;
        this.canceledAt = canceledAt;
        this.activeRequest = null;
    }

    public void complete() {
        if (status != SpeakingQueueStatus.ASSIGNED) {
            throw new IllegalStateException(
                    "Only assigned speaking requests can be completed."
            );
        }

        this.status = SpeakingQueueStatus.COMPLETED;
        this.activeRequest = null;
    }
}
