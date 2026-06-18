package com.sisibibi.api.global.websocket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AfterCommitWebSocketEventPublisherTest {

    private final WebSocketEventPublisher webSocketEventPublisher = mock(WebSocketEventPublisher.class);
    private final AfterCommitWebSocketEventPublisher publisher =
            new AfterCommitWebSocketEventPublisher(webSocketEventPublisher);

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishAfterCommit_registersPublishUntilCommit_whenSynchronizationIsActive() {
        TransactionSynchronizationManager.initSynchronization();
        Object event = new Object();

        publisher.publishAfterCommit("/topic/rooms/1/room/events", event);

        verify(webSocketEventPublisher, never())
                .publish("/topic/rooms/1/room/events", event);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCommit());
        verify(webSocketEventPublisher)
                .publish("/topic/rooms/1/room/events", event);
    }

    @Test
    void publishAfterCommit_publishesImmediately_whenSynchronizationIsInactive() {
        Object event = new Object();

        publisher.publishAfterCommit("/topic/rooms/1/room/events", event);

        verify(webSocketEventPublisher)
                .publish("/topic/rooms/1/room/events", event);
    }

    @Test
    void publishAfterCommit_doesNotThrow_whenPublisherFails() {
        Object event = new Object();
        org.mockito.BDDMockito.willThrow(new RuntimeException("broker down"))
                .given(webSocketEventPublisher)
                .publish("/topic/rooms/1/room/events", event);

        publisher.publishAfterCommit("/topic/rooms/1/room/events", event);
    }
}
