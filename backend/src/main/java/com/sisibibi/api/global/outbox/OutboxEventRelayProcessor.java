package com.sisibibi.api.global.outbox;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxEventRelayProcessor {

  private final OutboxEventRepository outboxEventRepository;

  @Transactional
  public List<OutboxEvent> claimReadyEvents(int batchSize) {
    LocalDateTime now = LocalDateTime.now();

    List<OutboxEvent> events = outboxEventRepository.findReadyEventsForUpdate(
        OutboxEventStatus.PENDING,
        now,
        PageRequest.of(0, batchSize)
    );

    events.forEach(event -> event.markPublishing(now));
    return events;
  }

  @Transactional
  public void markSent(Long eventId) {
    OutboxEvent event = outboxEventRepository.findByIdForUpdate(eventId);
    event.markSent(LocalDateTime.now());
  }

  @Transactional
  public void markRetryOrFailed(
      Long eventId,
      String errorMessage,
      int maxRetryCount
  ) {
    OutboxEvent event = outboxEventRepository.findByIdForUpdate(eventId);
    LocalDateTime now = LocalDateTime.now();

    if (event.getRetryCount() + 1 >= maxRetryCount) {
      event.markFailed(errorMessage, now);
      return;
    }

    event.markRetry(
        errorMessage,
        now.plusSeconds(nextRetryDelaySeconds(event.getRetryCount())),
        now
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
}
