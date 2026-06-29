package com.sisibibi.api.domain.payment.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisibibi.api.global.outbox.OutboxEvent;
import com.sisibibi.api.global.outbox.OutboxEventRepository;
import com.sisibibi.api.global.outbox.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomAiReportGenerationOutboxWriter {

  private static final String AGGREGATE_TYPE = "CUSTOM_AI_REPORT_REQUEST";
  private static final String DEDUPLICATION_KEY_PREFIX = "CUSTOM_AI_REPORT_GENERATION_REQUESTED:";

  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  @Transactional(propagation = Propagation.MANDATORY)
  public void record(Long customAiReportRequestId, LocalDateTime occurredAt) {
    String deduplicationKey = DEDUPLICATION_KEY_PREFIX + customAiReportRequestId;

    if (outboxEventRepository.existsByDeduplicationKey(deduplicationKey)) {
      return;
    }

    outboxEventRepository.save(OutboxEvent.pending(
        AGGREGATE_TYPE,
        customAiReportRequestId,
        OutboxEventType.CUSTOM_AI_REPORT_GENERATION_REQUESTED,
        payload(customAiReportRequestId),
        deduplicationKey,
        occurredAt
    ));
  }

  private String payload(Long customAiReportRequestId) {
    try {
      return objectMapper.writeValueAsString(Map.of(
          "customAiReportRequestId", customAiReportRequestId
      ));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize custom AI report request.", exception);
    }
  }
}