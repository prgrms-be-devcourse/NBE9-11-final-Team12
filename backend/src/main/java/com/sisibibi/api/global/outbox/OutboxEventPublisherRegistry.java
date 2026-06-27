package com.sisibibi.api.global.outbox;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisherRegistry {

  private final List<OutboxEventPublisher> publishers;

  public OutboxEventPublisher getPublisher(OutboxEventType eventType) {
    return publishers.stream()
        .filter(publisher -> publisher.supports(eventType))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "No outbox publisher found. eventType=" + eventType
        ));
  }
}