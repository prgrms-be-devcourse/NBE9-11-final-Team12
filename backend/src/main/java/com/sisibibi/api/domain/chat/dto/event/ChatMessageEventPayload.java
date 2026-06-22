package com.sisibibi.api.domain.chat.dto.event;

import com.sisibibi.api.domain.chat.entity.ChatMessage;
import java.time.LocalDateTime;

public record ChatMessageEventPayload(
        ChatEventType type,
        Long messageId,
        Long roomId,
        Long userId,
        String nicknameSnapshot,
        String content,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {

    public static ChatMessageEventPayload created(ChatMessage message) {
        return new ChatMessageEventPayload(
                ChatEventType.MESSAGE_CREATED,
                message.getId(),
                message.getRoomId(),
                message.getUserId(),
                message.getNicknameSnapshot(),
                message.getContent(),
                message.getCreatedAt(),
                message.getDeletedAt()
        );
    }

    public static ChatMessageEventPayload deleted(ChatMessage message) {
        return new ChatMessageEventPayload(
                ChatEventType.MESSAGE_DELETED,
                message.getId(),
                message.getRoomId(),
                message.getUserId(),
                message.getNicknameSnapshot(),
                null,
                message.getCreatedAt(),
                message.getDeletedAt()
        );
    }
}
