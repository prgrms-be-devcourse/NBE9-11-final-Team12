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
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_speaking_queue_room_order",
                        columnNames = {"room_id", "queue_order"}
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

    @Column(name = "queue_order", nullable = false)
    private int queueOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SpeakingQueueStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    private SpeakingQueue(
            Long roomId,
            Long userId,
            int queueOrder,
            LocalDateTime requestedAt
    ) {
        this.roomId = roomId;
        this.userId = userId;
        this.queueOrder = queueOrder;
        this.status = SpeakingQueueStatus.WAITING;
        this.requestedAt = requestedAt;
    }

    public static SpeakingQueue waiting(
            Long roomId,
            Long userId,
            int queueOrder,
            LocalDateTime requestedAt
    ) {
        return new SpeakingQueue(roomId, userId, queueOrder, requestedAt);
    }

    public void cancel(LocalDateTime canceledAt) {
        this.status = SpeakingQueueStatus.CANCELED;
        this.canceledAt = canceledAt;
    }
}
