package com.sisibibi.api.global.outbox;

public interface OutboxEventPublisher {

  boolean supports(OutboxEventType eventType);

  void publish(OutboxEvent event);
}