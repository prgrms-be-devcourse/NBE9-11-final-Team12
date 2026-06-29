package com.sisibibi.api.domain.report.worker;

import com.sisibibi.api.domain.report.queue.AiReportQueueProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiReportWorkerQueueMonitor {

    private final SqsClient sqsClient;
    private final AiReportQueueProperties properties;

    public QueueDepth getQueueDepth() {
        if (!StringUtils.hasText(properties.getQueueUrl())) {
            throw new IllegalStateException("AI report SQS queue URL is missing.");
        }

        Map<QueueAttributeName, String> attributes = sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                .queueUrl(properties.getQueueUrl())
                .attributeNames(
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                        QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE
                )
                .build()).attributes();

        return new QueueDepth(
                parseCount(attributes.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)),
                parseCount(attributes.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE))
        );
    }

    private int parseCount(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        return Integer.parseInt(value);
    }

    public record QueueDepth(int visible, int notVisible) {

        public boolean isIdle() {
            return visible == 0 && notVisible == 0;
        }
    }
}
