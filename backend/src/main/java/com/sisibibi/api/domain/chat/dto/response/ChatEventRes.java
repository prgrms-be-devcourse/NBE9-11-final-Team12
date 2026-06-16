package com.sisibibi.api.domain.chat.dto.response;

import com.sisibibi.api.domain.chat.entity.ChatEventType;
import com.sisibibi.api.domain.chat.entity.ChatMessage;
import java.time.LocalDateTime;

public record ChatEventRes(
        ChatEventType type,
        Long messageId,
        Long roomId,
        Long userId,
        String nicknameSnapshot,
        String content,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {

    public static ChatEventRes created(ChatMessage message) {
        return new ChatEventRes(
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
}
