package com.sisibibi.api.domain.chat.service;

import com.sisibibi.api.domain.chat.dto.response.ChatEventRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class AfterCommitChatMessagePublisher {

    private final ChatMessagePublisher chatMessagePublisher;

    public void publishAfterCommit(ChatEventRes event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishSafely(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishSafely(event);
            }
        });
    }

    private void publishSafely(ChatEventRes event) {
        try {
            chatMessagePublisher.publish(event);
        } catch (RuntimeException publishException) {
            log.warn(
                    "Failed to publish chat event after commit. roomId={}, messageId={}, type={}",
                    event.roomId(),
                    event.messageId(),
                    event.type(),
                    publishException
            );
        }
    }
}
