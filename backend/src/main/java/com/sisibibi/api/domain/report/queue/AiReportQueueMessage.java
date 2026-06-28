package com.sisibibi.api.domain.report.queue;

import com.sisibibi.api.domain.report.service.AiReportGenerationType;

public record AiReportQueueMessage(
        Long reportId,
        Long roomId,
        AiReportGenerationType generationType,
        String idempotencyKey
) {

    public static AiReportQueueMessage of(Long reportId, Long roomId, AiReportGenerationType generationType) {
        return new AiReportQueueMessage(
                reportId,
                roomId,
                generationType,
                "ai-report-" + reportId + "-v1"
        );
    }
}
