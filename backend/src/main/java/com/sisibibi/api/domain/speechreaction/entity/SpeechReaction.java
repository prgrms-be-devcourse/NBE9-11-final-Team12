package com.sisibibi.api.domain.speechreaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
        name = "speech_reactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_speech_reactions_speech_user",
                columnNames = {"speech_id", "user_id"}
        ),
        indexes = @Index(
                name = "idx_speech_reactions_speech_id",
                columnList = "speech_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpeechReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "speech_id", nullable = false)
    private Long speechId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private SpeechReaction(Long speechId, Long userId) {
        this.speechId = speechId;
        this.userId = userId;
    }

    public static SpeechReaction create(Long speechId, Long userId) {
        return new SpeechReaction(speechId, userId);
    }
}
