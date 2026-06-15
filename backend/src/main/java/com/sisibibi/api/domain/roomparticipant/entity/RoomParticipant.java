package com.sisibibi.api.domain.roomparticipant.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
    name = "room_participants",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_room_participants_room_id_user_id",
        columnNames = {"room_id", "user_id"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomParticipantStatus status;

    public static RoomParticipant join(Long roomId, Long userId) {
        RoomParticipant participant = new RoomParticipant();
        participant.roomId = roomId;
        participant.userId = userId;
        participant.joinedAt = LocalDateTime.now();
        participant.leftAt = null;
        participant.status = RoomParticipantStatus.JOINED;
        return participant;
    }

    public void rejoin() {
        this.joinedAt = LocalDateTime.now();
        this.leftAt = null;
        this.status = RoomParticipantStatus.JOINED;
    }

    public void leave() {
        if (this.status == RoomParticipantStatus.LEFT) {
            return;
        }

        this.leftAt = LocalDateTime.now();
        this.status = RoomParticipantStatus.LEFT;
    }
}
