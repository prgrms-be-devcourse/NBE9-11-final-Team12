package com.sisibibi.api.global.outbox;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

  private final OutboxEventRepository outboxEventRepository;
  private final OutboxEventPublisherRegistry publisherRegistry;

  @Scheduled(fixedDelayString = "${app.outbox.relay.fixed-delay-ms:30000}")
  @Transactional
  public void publishReadyEvents() {
    LocalDateTime now = LocalDateTime.now();

    var events = outboxEventRepository.findReadyEventsForUpdate(
        OutboxEventStatus.PENDING,
        now,
        PageRequest.of(0, DEFAULT_BATCH_SIZE)
    );

    if (events.isEmpty()) {
      return;
    }

    for (OutboxEvent event : events) {
      publishOne(event, now);
    }

    log.info("Outbox relay processed events. count={}", events.size());
  }

  private void publishOne(OutboxEvent event, LocalDateTime now) {
    try {
      event.markPublishing(now);

      OutboxEventPublisher publisher =
          publisherRegistry.getPublisher(event.getEventType());

      publisher.publish(event);
      event.markSent(LocalDateTime.now());
    } catch (RuntimeException exception) {
      handleFailure(event, exception);
    }
  }

  private void handleFailure(OutboxEvent event, RuntimeException exception) {
    LocalDateTime now = LocalDateTime.now();
    String errorMessage = abbreviate(exception.getMessage());

    if (event.getRetryCount() + 1 >= MAX_RETRY_COUNT) {
      event.markFailed(errorMessage, now);
      log.error(
          "Outbox event permanently failed. eventId={}, eventType={}",
          event.getEventId(),
          event.getEventType(),
          exception
      );
      return;
    }

    event.markRetry(
        errorMessage,
        now.plusSeconds(nextRetryDelaySeconds(event.getRetryCount())),
        now
    );

    log.warn(
        "Outbox event publish failed. eventId={}, eventType={}, retryCount={}",
        event.getEventId(),
        event.getEventType(),
        event.getRetryCount(),
        exception
    );
  }

  private long nextRetryDelaySeconds(int retryCount) {
    return switch (retryCount) {
      case 0 -> 10;
      case 1 -> 30;
      case 2 -> 60;
      case 3 -> 180;
      default -> 300;
    };
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