package com.sisibibi.api.global.outbox;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.outbox.relay",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class OutboxRelay {

  private static final int DEFAULT_BATCH_SIZE = 20;
  private static final int MAX_RETRY_COUNT = 5;

  private final OutboxEventRelayProcessor relayProcessor;
  private final OutboxEventPublisherRegistry publisherRegistry;

  @Scheduled(fixedDelayString = "${app.outbox.relay.fixed-delay-ms:30000}")
  public void publishReadyEvents() {
    List<OutboxEvent> events = relayProcessor.claimReadyEvents(DEFAULT_BATCH_SIZE);

    if (events.isEmpty()) {
      return;
    }

    for (OutboxEvent event : events) {
      publishOne(event);
    }

    log.info("Outbox relay processed events. count={}", events.size());
  }

  private void publishOne(OutboxEvent event) {
    try {
      OutboxEventPublisher publisher =
          publisherRegistry.getPublisher(event.getEventType());

      publisher.publish(event);
      relayProcessor.markSent(event.getId());
    } catch (RuntimeException exception) {
      String errorMessage = abbreviate(exception.getMessage());
      relayProcessor.markRetryOrFailed(
          event.getId(),
          errorMessage,
          MAX_RETRY_COUNT
      );

      log.warn(
          "Outbox event publish failed. eventId={}, eventType={}, retryCount={}",
          event.getEventId(),
          event.getEventType(),
          event.getRetryCount(),
          exception
      );
    }
  }

  private String abbreviate(String message) {
    if (message == null || message.isBlank()) {
      return "";
    }
    int maxLength = 500;
    return message.length() <= maxLength
        ? message
        : message.substring(0, maxLength);
  }
}