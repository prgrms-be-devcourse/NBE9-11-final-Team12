package com.sisibibi.api.domain.speech.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "room_queue_sequences")
public class RoomQueueSequence {

    @Id
    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "next_queue_order", nullable = false)
    private Integer nextQueueOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private RoomQueueSequence(Long roomId, LocalDateTime now) {
        this.roomId = roomId;
        this.nextQueueOrder = 1;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static RoomQueueSequence create(Long roomId, LocalDateTime now) {
        return new RoomQueueSequence(roomId, now);
    }

    public int issueNextQueueOrder(LocalDateTime issuedAt) {
        int issuedQueueOrder = nextQueueOrder;
        nextQueueOrder += 1;
        updatedAt = issuedAt;
        return issuedQueueOrder;
    }
}
