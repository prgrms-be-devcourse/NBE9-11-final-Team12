package com.sisibibi.api.domain.report.worker;

import com.sisibibi.api.domain.report.queue.AiReportQueueProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AiReportWorkerQueueMonitorTest {

    private final SqsClient sqsClient = mock(SqsClient.class);
    private final AiReportQueueProperties properties = new AiReportQueueProperties();
    private final AiReportWorkerQueueMonitor monitor = new AiReportWorkerQueueMonitor(sqsClient, properties);

    @Test
    void getQueueDepth_readsVisibleAndNotVisibleCounts() {
        properties.setQueueUrl("https://sqs.ap-northeast-2.amazonaws.com/123/team12-speech-event-queue");
        given(sqsClient.getQueueAttributes(org.mockito.ArgumentMatchers.any(GetQueueAttributesRequest.class)))
                .willReturn(GetQueueAttributesResponse.builder()
                        .attributes(Map.of(
                                QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES, "3",
                                QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE, "1"
                        ))
                        .build());

        AiReportWorkerQueueMonitor.QueueDepth queueDepth = monitor.getQueueDepth();

        assertThat(queueDepth.visible()).isEqualTo(3);
        assertThat(queueDepth.notVisible()).isEqualTo(1);
        assertThat(queueDepth.isIdle()).isFalse();

        ArgumentCaptor<GetQueueAttributesRequest> requestCaptor =
                ArgumentCaptor.forClass(GetQueueAttributesRequest.class);
        verify(sqsClient).getQueueAttributes(requestCaptor.capture());
        assertThat(requestCaptor.getValue().queueUrl()).isEqualTo(properties.getQueueUrl());
        assertThat(requestCaptor.getValue().attributeNames()).containsExactlyInAnyOrder(
                QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE
        );
    }

    @Test
    void getQueueDepth_failsFast_whenQueueUrlIsMissing() {
        properties.setQueueUrl("");

        assertThatThrownBy(monitor::getQueueDepth)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AI report SQS queue URL is missing.");
        verifyNoInteractions(sqsClient);
    }
}
