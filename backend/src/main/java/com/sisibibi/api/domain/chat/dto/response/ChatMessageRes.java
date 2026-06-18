package com.sisibibi.api.domain.chat.dto.response;

import com.sisibibi.api.domain.chat.entity.ChatMessage;
import java.time.LocalDateTime;

public record ChatMessageRes(
        Long messageId,
        Long roomId,
        Long userId,
        String nicknameSnapshot,
        String content,
        LocalDateTime createdAt
) {

    public static ChatMessageRes from(ChatMessage message) {
        return new ChatMessageRes(
                message.getId(),
                message.getRoomId(),
                message.getUserId(),
                message.getNicknameSnapshot(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
