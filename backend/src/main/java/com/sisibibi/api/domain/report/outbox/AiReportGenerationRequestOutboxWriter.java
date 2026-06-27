package com.sisibibi.api.domain.report.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisibibi.api.global.outbox.OutboxEvent;
import com.sisibibi.api.global.outbox.OutboxEventRepository;
import com.sisibibi.api.global.outbox.OutboxEventType;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiReportGenerationRequestOutboxWriter {

  private static final String AGGREGATE_TYPE = "ROOM";
  private static final String DEDUPLICATION_KEY_PREFIX = "AI_REPORT_GENERATION_REQUESTED:";

  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  @Transactional(propagation = Propagation.MANDATORY)
  public void record(Long roomId, LocalDateTime closedAt) {
    String deduplicationKey = DEDUPLICATION_KEY_PREFIX + roomId;

    if (outboxEventRepository.existsByDeduplicationKey(deduplicationKey)) {
      return;
    }

    outboxEventRepository.save(OutboxEvent.pending(
        AGGREGATE_TYPE,
        roomId,
        OutboxEventType.AI_REPORT_GENERATION_REQUESTED,
        payload(roomId, closedAt),
        deduplicationKey,
        closedAt
    ));
  }

  private String payload(Long roomId, LocalDateTime closedAt) {
    try {
      return objectMapper.writeValueAsString(Map.of(
          "roomId", roomId,
          "closedAt", closedAt.toString()
      ));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize AI report generation request.", exception);
    }
  }
}