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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "speeches",
        indexes = @Index(
                name = "idx_speeches_room_id_id",
                columnList = "room_id, id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Speech {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private SpeechStance stance;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpeechStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Speech(Long roomId, Long userId, String content, SpeechStance stance) {
        LocalDateTime now = LocalDateTime.now();
        this.roomId = roomId;
        this.userId = userId;
        this.content = content;
        this.stance = stance;
        this.status = SpeechStatus.READY;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static Speech createMainOpinion(
            Long roomId,
            Long userId,
            String content,
            SpeechStance stance
    ) {
        return new Speech(roomId, userId, content, stance);
    }

    public void updateMainOpinion(String content, SpeechStance stance) {
        this.content = content;
        this.stance = stance;
        this.updatedAt = LocalDateTime.now();
    }
}
