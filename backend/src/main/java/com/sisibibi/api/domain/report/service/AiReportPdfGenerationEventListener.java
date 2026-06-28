package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.dto.event.AiReportPdfGenerationRequestedEvent;
import com.sisibibi.api.global.config.AsyncConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AiReportPdfGenerationEventListener {

    private final AiReportPdfGenerationService generationService;

    @Async(AsyncConfig.DOMAIN_EVENT_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AiReportPdfGenerationRequestedEvent event) {
        generationService.generate(event.exportId());
    }
}
