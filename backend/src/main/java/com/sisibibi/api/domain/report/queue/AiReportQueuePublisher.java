package com.sisibibi.api.domain.report.queue;

public interface AiReportQueuePublisher {

    void publish(AiReportQueueMessage message);
}
