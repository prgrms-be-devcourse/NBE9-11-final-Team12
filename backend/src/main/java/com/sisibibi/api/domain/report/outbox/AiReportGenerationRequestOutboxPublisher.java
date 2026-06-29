package com.sisibibi.api.domain.report.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisibibi.api.domain.report.queue.AiReportQueueMessage;
import com.sisibibi.api.domain.report.queue.AiReportQueuePublisher;
import com.sisibibi.api.domain.report.service.AiReportGenerationType;
import com.sisibibi.api.domain.report.service.AiReportPersistenceService;
import com.sisibibi.api.domain.report.service.AiReportRequestResult;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.outbox.OutboxEvent;
import com.sisibibi.api.global.outbox.OutboxEventPublisher;
import com.sisibibi.api.global.outbox.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiReportGenerationRequestOutboxPublisher implements OutboxEventPublisher {

  private final ObjectMapper objectMapper;
  private final AiReportPersistenceService aiReportPersistenceService;
  private final AiReportQueuePublisher aiReportQueuePublisher;

  @Override
  public boolean supports(OutboxEventType eventType) {
    return eventType == OutboxEventType.AI_REPORT_GENERATION_REQUESTED;
  }

  // 이벤트 발행
  @Override
  public void publish(OutboxEvent event) {
    Long roomId = roomIdFrom(event);
    AiReportRequestResult result = aiReportPersistenceService.requestBaseGenerationFromRoomClose(roomId);

    if (!result.shouldPublish()) {
      return;
    }

    Long reportId = result.response().reportId();

    try {
      aiReportQueuePublisher.publish(AiReportQueueMessage.of(
          reportId,
          roomId,
          AiReportGenerationType.BASE_ONLY
      ));
      aiReportPersistenceService.markQueued(reportId);
    } catch (RuntimeException publishException) {
      aiReportPersistenceService.markPublishFailed(
          reportId,
          ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.name(),
          ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.getMessage()
      );
      throw publishException;
    }
  }

  private Long roomIdFrom(OutboxEvent event) {
    try {
      JsonNode payload = objectMapper.readTree(event.getPayload());
      return payload.get("roomId").asLong();
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(
          "Failed to deserialize AI report generation request.",
          exception
      );
    }
  }
}