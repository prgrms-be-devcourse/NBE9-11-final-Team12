package com.sisibibi.api.domain.report.queue;

import com.sisibibi.api.domain.report.dto.event.AiReportGenerationRequestedEvent;
import com.sisibibi.api.domain.report.service.AiReportGenerationType;
import com.sisibibi.api.domain.report.service.AiReportPersistenceService;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class AiReportQueuePublishEventListenerTest {

    @Mock
    private AiReportQueuePublisher aiReportQueuePublisher;

    @Mock
    private AiReportPersistenceService aiReportPersistenceService;

    @InjectMocks
    private AiReportQueuePublishEventListener listener;

    @Test
    void handle_marksQueued_whenQueuePublishSucceeds() {
        AiReportGenerationRequestedEvent event =
                new AiReportGenerationRequestedEvent(55L, 10L, AiReportGenerationType.BASE_ONLY);

        listener.handle(event);

        ArgumentCaptor<AiReportQueueMessage> messageCaptor = ArgumentCaptor.forClass(AiReportQueueMessage.class);
        verify(aiReportQueuePublisher).publish(messageCaptor.capture());
        assertThat(messageCaptor.getValue().reportId()).isEqualTo(55L);
        assertThat(messageCaptor.getValue().roomId()).isEqualTo(10L);
        assertThat(messageCaptor.getValue().generationType()).isEqualTo(AiReportGenerationType.BASE_ONLY);
        assertThat(messageCaptor.getValue().idempotencyKey()).isEqualTo("ai-report-55-v1");
        verify(aiReportPersistenceService).markQueued(55L);
        verifyNoMoreInteractions(aiReportPersistenceService);
    }

    @Test
    void handle_marksPublishFailed_whenQueuePublishFails() {
        AiReportGenerationRequestedEvent event =
                new AiReportGenerationRequestedEvent(55L, 10L, AiReportGenerationType.BASE_WITH_CUSTOM);
        doThrow(new IllegalStateException("queue url missing"))
                .when(aiReportQueuePublisher)
                .publish(event.toQueueMessage());

        listener.handle(event);

        verify(aiReportPersistenceService).markPublishFailed(
                55L,
                ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.name(),
                ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.getMessage()
        );
    }
}
