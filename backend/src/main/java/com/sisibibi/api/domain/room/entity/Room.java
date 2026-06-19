package com.sisibibi.api.domain.room.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "rooms",
    uniqueConstraints = @UniqueConstraint(name = "uk_rooms_topic_id", columnNames = "topic_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "max_participants", nullable = false)
    private int maxParticipants;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Room open(Long topicId, String title, LocalDateTime startedAt, LocalDateTime endedAt, int maxParticipants) {
        Room room = new Room();
        room.topicId = topicId;
        room.title = title;
        room.status = RoomStatus.OPEN;
        room.startedAt = startedAt;
        room.endedAt = endedAt;
        room.maxParticipants = maxParticipants;
        return room;
    }

    public void close(LocalDateTime closedAt) {
        if (this.status == RoomStatus.CLOSED) {
            return;
        }

        this.status = RoomStatus.CLOSED;
        this.endedAt = closedAt;
    }

    public void update(String title, LocalDateTime startedAt, LocalDateTime endedAt, Integer maxParticipants) {
        if (title != null) {
            this.title = title;
        }

        if (startedAt != null) {
            this.startedAt = startedAt;
        }

        if (endedAt != null) {
            this.endedAt = endedAt;
        }

        if (maxParticipants != null) {
            this.maxParticipants = maxParticipants;
        }
    }
}
