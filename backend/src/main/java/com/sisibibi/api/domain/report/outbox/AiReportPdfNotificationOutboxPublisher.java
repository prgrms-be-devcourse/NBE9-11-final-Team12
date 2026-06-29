package com.sisibibi.api.domain.report.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisibibi.api.domain.report.service.AiReportPdfGenerationService;
import com.sisibibi.api.global.outbox.OutboxEvent;
import com.sisibibi.api.global.outbox.OutboxEventPublisher;
import com.sisibibi.api.global.outbox.OutboxEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiReportPdfNotificationOutboxPublisher implements OutboxEventPublisher {

    private final ObjectMapper objectMapper;
    private final AiReportPdfGenerationService generationService;

    @Override
    public boolean supports(OutboxEventType eventType) {
        return eventType == OutboxEventType.AI_REPORT_PDF_NOTIFICATION_REQUESTED;
    }

    @Override
    public void publish(OutboxEvent event) {
        generationService.retryNotification(exportIdFrom(event));
    }

    private Long exportIdFrom(OutboxEvent event) {
        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());
            return payload.get("exportId").asLong();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize AI report PDF notification request.", exception);
        }
    }
}
