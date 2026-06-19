package com.sisibibi.api.domain.speech.entity;

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

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private Speech(Long roomId, Long userId, String content, SpeechStance stance) {
        this.roomId = roomId;
        this.userId = userId;
        this.content = content;
        this.stance = stance;
        this.status = SpeechStatus.READY;
        this.deleted = false;
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
    }

    public void updateLink(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public void softDelete(LocalDateTime deletedAt) {
        if (this.deleted) {
            return;
        }

        this.deleted = true;
        this.deletedAt = deletedAt;
    }
}
