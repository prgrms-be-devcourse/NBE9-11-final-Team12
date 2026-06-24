package com.sisibibi.api.domain.report.dto.event;

import com.sisibibi.api.domain.report.queue.AiReportQueueMessage;
import com.sisibibi.api.domain.report.service.AiReportGenerationType;

public record AiReportGenerationRequestedEvent(
        Long reportId,
        Long roomId,
        AiReportGenerationType generationType
) {

    public AiReportQueueMessage toQueueMessage() {
        return AiReportQueueMessage.of(reportId, roomId, generationType);
    }
}
