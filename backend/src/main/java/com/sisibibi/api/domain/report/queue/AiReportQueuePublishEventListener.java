package com.sisibibi.api.domain.report.queue;

import com.sisibibi.api.domain.report.dto.event.AiReportGenerationRequestedEvent;
import com.sisibibi.api.domain.report.service.AiReportPersistenceService;
import com.sisibibi.api.domain.report.worker.AiReportWorkerEc2Service;
import com.sisibibi.api.global.config.AsyncConfig;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiReportQueuePublishEventListener {

    private final AiReportQueuePublisher aiReportQueuePublisher;
    private final AiReportPersistenceService aiReportPersistenceService;
    private final AiReportWorkerEc2Service aiReportWorkerEc2Service;

    @Async(AsyncConfig.DOMAIN_EVENT_TASK_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(AiReportGenerationRequestedEvent event) {
        try {
            aiReportQueuePublisher.publish(event.toQueueMessage());
            aiReportPersistenceService.markQueued(event.reportId());
            aiReportWorkerEc2Service.startWorkerIfEnabled();
        } catch (RuntimeException publishException) {
            log.warn(
                    "Failed to publish AI report generation message. reportId={}, roomId={}, generationType={}",
                    event.reportId(),
                    event.roomId(),
                    event.generationType(),
                    publishException
            );
            aiReportPersistenceService.markPublishFailed(
                    event.reportId(),
                    ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.name(),
                    ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.getMessage()
            );
        }
    }
}
