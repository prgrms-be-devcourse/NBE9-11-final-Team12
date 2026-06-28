package com.sisibibi.api.domain.report.scheduler;

import com.sisibibi.api.domain.report.service.AiReportPublishRetryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.ai-report.queue.retry",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class AiReportPublishRetryScheduler {

    private final AiReportPublishRetryService aiReportPublishRetryService;

    @Scheduled(
            fixedDelayString =
                    "${app.ai-report.queue.retry.fixed-delay-ms:30000}"
    )
    public void republishStaleRequests() {
        try {
            int count = aiReportPublishRetryService.republishStaleRequests();
            if (count > 0) {
                log.info("Republished stale AI report requests. count={}", count);
            }
        } catch (RuntimeException retryException) {
            log.error("Failed to retry stale AI report publish requests.", retryException);
        }
    }
}
