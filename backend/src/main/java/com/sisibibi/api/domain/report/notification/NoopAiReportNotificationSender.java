package com.sisibibi.api.domain.report.notification;

import com.sisibibi.api.domain.report.service.AiReportNotificationSender;
import com.sisibibi.api.domain.report.service.AiReportPdfReadyCommand;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.ai-report.notification", name = "provider", havingValue = "noop")
public class NoopAiReportNotificationSender implements AiReportNotificationSender {

    @Override
    public void sendPdfReady(AiReportPdfReadyCommand command) {
        // Intentionally empty for local development and tests.
    }
}
