package com.sisibibi.api.domain.report.outbox;

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
public class AiReportPdfNotificationOutboxWriter {

    private static final String AGGREGATE_TYPE = "AI_REPORT_PDF_EXPORT";
    private static final String DEDUPLICATION_KEY_PREFIX = "AI_REPORT_PDF_NOTIFICATION_REQUESTED:";

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRED)
    public void record(Long exportId, LocalDateTime requestedAt) {
        String deduplicationKey = DEDUPLICATION_KEY_PREFIX + exportId;

        if (outboxEventRepository.existsByDeduplicationKey(deduplicationKey)) {
            return;
        }

        outboxEventRepository.save(OutboxEvent.pending(
                AGGREGATE_TYPE,
                exportId,
                OutboxEventType.AI_REPORT_PDF_NOTIFICATION_REQUESTED,
                payload(exportId),
                deduplicationKey,
                requestedAt
        ));
    }

    private String payload(Long exportId) {
        try {
            return objectMapper.writeValueAsString(Map.of("exportId", exportId));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize AI report PDF notification request.", exception);
        }
    }
}
