package com.sisibibi.api.domain.chat.service;

import com.sisibibi.api.domain.chat.dto.response.ChatEventRes;
import com.sisibibi.api.global.websocket.AfterCommitWebSocketEventPublisher;
import com.sisibibi.api.global.websocket.RoomWebSocketDestinations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AfterCommitChatMessagePublisher {

    private final AfterCommitWebSocketEventPublisher afterCommitWebSocketEventPublisher;

    public void publishAfterCommit(ChatEventRes event) {
        afterCommitWebSocketEventPublisher.publishAfterCommit(
                RoomWebSocketDestinations.chatMessages(event.roomId()),
                event
        );
    }
}
