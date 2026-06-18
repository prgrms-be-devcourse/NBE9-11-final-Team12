package com.sisibibi.api.domain.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "chat_messages",
        indexes = {
                @Index(name = "idx_chat_messages_room_id_id", columnList = "room_id, id"),
                @Index(name = "idx_chat_messages_room_created_at", columnList = "room_id, created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "nickname_snapshot", nullable = false, length = 50)
    private String nicknameSnapshot;

    @Column(nullable = false, length = 300)
    private String content;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ChatMessage(Long roomId, Long userId, String nicknameSnapshot, String content) {
        this.roomId = roomId;
        this.userId = userId;
        this.nicknameSnapshot = nicknameSnapshot;
        this.content = content;
        this.deleted = false;
    }

    public static ChatMessage create(
            Long roomId,
            Long userId,
            String nicknameSnapshot,
            String content
    ) {
        return new ChatMessage(roomId, userId, nicknameSnapshot, content);
    }

    public void softDelete(Long deletedBy, LocalDateTime deletedAt) {
        if (this.deleted) {
            return;
        }

        this.deleted = true;
        this.deletedBy = deletedBy;
        this.deletedAt = deletedAt;
    }
}
