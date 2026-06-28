package com.sisibibi.api.domain.report.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
@RequiredArgsConstructor
public class SqsAiReportQueuePublisher implements AiReportQueuePublisher {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final AiReportQueueProperties properties;

    @Override
    public void publish(AiReportQueueMessage message) {
        if (!StringUtils.hasText(properties.getQueueUrl())) {
            throw new IllegalStateException("AI report SQS queue URL is missing.");
        }

        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(properties.getQueueUrl())
                .messageBody(toJson(message))
                .build());
    }

    private String toJson(AiReportQueueMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize AI report queue message.", e);
        }
    }
}
