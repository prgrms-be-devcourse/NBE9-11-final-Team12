package com.sisibibi.api.global.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class AfterCommitWebSocketEventPublisher {

    private final WebSocketEventPublisher webSocketEventPublisher;

    public void publishAfterCommit(String destination, Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishSafely(destination, event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishSafely(destination, event);
            }
        });
    }

    private void publishSafely(String destination, Object event) {
        try {
            webSocketEventPublisher.publish(destination, event);
        } catch (RuntimeException publishException) {
            log.warn(
                    "Failed to publish WebSocket event after commit. destination={}",
                    destination,
                    publishException
            );
        }
    }
}
