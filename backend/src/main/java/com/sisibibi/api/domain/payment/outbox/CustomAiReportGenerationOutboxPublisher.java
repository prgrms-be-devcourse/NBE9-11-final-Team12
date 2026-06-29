package com.sisibibi.api.domain.payment.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisibibi.api.domain.payment.entity.CustomAiReportRequest;
import com.sisibibi.api.domain.payment.repository.CustomAiReportRequestRepository;
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
public class CustomAiReportGenerationOutboxPublisher implements OutboxEventPublisher {

  private final ObjectMapper objectMapper;
  private final CustomAiReportRequestRepository customAiReportRequestRepository;
  private final AiReportPersistenceService aiReportPersistenceService;
  private final AiReportQueuePublisher aiReportQueuePublisher;

  @Override
  public boolean supports(OutboxEventType eventType) {
    return eventType == OutboxEventType.CUSTOM_AI_REPORT_GENERATION_REQUESTED;
  }

  @Override
  public void publish(OutboxEvent event) {
    CustomAiReportRequest request = customAiReportRequestRepository.findById(customRequestIdFrom(event))
        .orElseThrow(() -> new IllegalStateException("Custom AI report request not found."));

    AiReportRequestResult result = aiReportPersistenceService.requestCustomGeneration(
        request.getRoomId(),
        request.getUserId(),
        request.getCustomPrompts().stream()
            .map(prompt -> new com.sisibibi.api.domain.report.prompt.CustomPromptCommand(
                prompt.label(),
                prompt.prompt()
            ))
            .toList()
    );

    Long reportId = result.response().reportId();

    try {
      aiReportQueuePublisher.publish(new AiReportQueueMessage(
          reportId,
          request.getRoomId(),
          AiReportGenerationType.CUSTOM_ONLY,
          "custom-ai-report-request-" + request.getId() + "-v1"
      ));
      aiReportPersistenceService.markQueued(reportId);
      request.markQueued();
      customAiReportRequestRepository.save(request);
    } catch (RuntimeException publishException) {
      aiReportPersistenceService.markPublishFailed(
          reportId,
          ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.name(),
          ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.getMessage()
      );
      request.markPublishFailed(ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.getMessage());
      customAiReportRequestRepository.save(request);
      throw publishException;
    }
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