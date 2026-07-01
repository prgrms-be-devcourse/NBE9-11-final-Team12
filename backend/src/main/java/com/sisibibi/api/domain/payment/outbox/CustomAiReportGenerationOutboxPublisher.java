package com.sisibibi.api.domain.payment.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisibibi.api.domain.payment.service.CustomAiReportGenerationDispatchService;
import com.sisibibi.api.global.outbox.OutboxEvent;
import com.sisibibi.api.global.outbox.OutboxEventPublisher;
import com.sisibibi.api.global.outbox.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomAiReportGenerationOutboxPublisher implements OutboxEventPublisher {

  private final ObjectMapper objectMapper;
  private final CustomAiReportGenerationDispatchService dispatchService;

  @Override
  public boolean supports(OutboxEventType eventType) {
    return eventType == OutboxEventType.CUSTOM_AI_REPORT_GENERATION_REQUESTED;
  }

  @Override
  public void publish(OutboxEvent event) {
    dispatchService.dispatch(customRequestIdFrom(event));
  }

  private Long customRequestIdFrom(OutboxEvent event) {
    try {
      JsonNode payload = objectMapper.readTree(event.getPayload());
      return payload.get("customAiReportRequestId").asLong();
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize custom AI report request.", exception);
    }
  }
}
