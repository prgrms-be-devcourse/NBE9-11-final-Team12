package com.sisibibi.api.domain.chat.service;

import com.sisibibi.api.domain.chat.dto.response.ChatEventRes;
import com.sisibibi.api.domain.chat.entity.ChatEventType;
import com.sisibibi.api.global.websocket.AfterCommitWebSocketEventPublisher;
import com.sisibibi.api.global.websocket.RoomWebSocketDestinations;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AfterCommitChatMessagePublisherTest {

    private final AfterCommitWebSocketEventPublisher afterCommitWebSocketEventPublisher =
            mock(AfterCommitWebSocketEventPublisher.class);
    private final AfterCommitChatMessagePublisher publisher =
            new AfterCommitChatMessagePublisher(afterCommitWebSocketEventPublisher);

    @Test
    void publishAfterCommit_delegatesToCommonAfterCommitPublisher() {
        ChatEventRes event = event();

        publisher.publishAfterCommit(event);

        verify(afterCommitWebSocketEventPublisher).publishAfterCommit(
                RoomWebSocketDestinations.chatMessages(1L),
                event
        );
    }

    private ChatEventRes event() {
        return new ChatEventRes(
                ChatEventType.MESSAGE_CREATED,
                10L,
                1L,
                2L,
                "tester",
                "hello",
                LocalDateTime.of(2026, 6, 15, 10, 0),
                null
        );
    }
}
