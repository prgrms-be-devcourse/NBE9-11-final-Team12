package com.sisibibi.api.domain.report.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisibibi.api.domain.report.service.AiReportPdfGenerationService;
import com.sisibibi.api.global.config.AsyncConfig;
import com.sisibibi.api.global.outbox.OutboxEvent;
import com.sisibibi.api.global.outbox.OutboxEventPublisher;
import com.sisibibi.api.global.outbox.OutboxEventType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;

@Component
public class AiReportPdfGenerationOutboxPublisher implements OutboxEventPublisher {

    private final ObjectMapper objectMapper;
    private final AiReportPdfGenerationService generationService;
    private final Executor taskExecutor;

    public AiReportPdfGenerationOutboxPublisher(
            ObjectMapper objectMapper,
            AiReportPdfGenerationService generationService,
            @Qualifier(AsyncConfig.DOMAIN_EVENT_TASK_EXECUTOR) Executor taskExecutor
    ) {
        this.objectMapper = objectMapper;
        this.generationService = generationService;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public boolean supports(OutboxEventType eventType) {
        return eventType == OutboxEventType.AI_REPORT_PDF_GENERATION_REQUESTED;
    }

    @Override
    public void publish(OutboxEvent event) {
        Long exportId = exportIdFrom(event);
        taskExecutor.execute(() -> generationService.generate(exportId));
    }

    private Long exportIdFrom(OutboxEvent event) {
        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());
            return payload.get("exportId").asLong();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize AI report PDF generation request.", exception);
        }
    }
}
