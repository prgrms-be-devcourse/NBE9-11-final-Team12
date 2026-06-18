package com.sisibibi.api.domain.chat.service;

import com.sisibibi.api.domain.chat.dto.response.ChatEventRes;
import com.sisibibi.api.domain.chat.entity.ChatEventType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AfterCommitChatMessagePublisherTest {

    private final ChatMessagePublisher chatMessagePublisher = mock(ChatMessagePublisher.class);
    private final AfterCommitChatMessagePublisher publisher =
            new AfterCommitChatMessagePublisher(chatMessagePublisher);

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishAfterCommit_registersPublishUntilCommit_whenSynchronizationIsActive() {
        TransactionSynchronizationManager.initSynchronization();
        ChatEventRes event = event();

        publisher.publishAfterCommit(event);

        verify(chatMessagePublisher, never()).publish(event);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCommit());
        verify(chatMessagePublisher).publish(event);
    }

    @Test
    void publishAfterCommit_publishesImmediately_whenSynchronizationIsInactive() {
        ChatEventRes event = event();

        publisher.publishAfterCommit(event);

        verify(chatMessagePublisher).publish(event);
    }

    @Test
    void publishAfterCommit_doesNotThrow_whenPublisherFails() {
        ChatEventRes event = event();
        org.mockito.BDDMockito.willThrow(new RuntimeException("broker down"))
                .given(chatMessagePublisher)
                .publish(event);

        publisher.publishAfterCommit(event);
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
