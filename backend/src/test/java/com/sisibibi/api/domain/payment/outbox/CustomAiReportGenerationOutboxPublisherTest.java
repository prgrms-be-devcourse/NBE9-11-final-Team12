package com.sisibibi.api.domain.payment.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisibibi.api.domain.payment.service.CustomAiReportGenerationDispatchService;
import com.sisibibi.api.global.outbox.OutboxEvent;
import com.sisibibi.api.global.outbox.OutboxEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomAiReportGenerationOutboxPublisherTest {

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private CustomAiReportGenerationDispatchService dispatchService;

  @InjectMocks
  private CustomAiReportGenerationOutboxPublisher publisher;

  @Test
  void publish_delegatesCustomReportDispatch() throws Exception {
    OutboxEvent event = OutboxEvent.pending(
        "CUSTOM_AI_REPORT_REQUEST",
        44L,
        OutboxEventType.CUSTOM_AI_REPORT_GENERATION_REQUESTED,
        "{\"customAiReportRequestId\":44}",
        "CUSTOM_AI_REPORT_GENERATION_REQUESTED:44",
        LocalDateTime.of(2026, 6, 30, 1, 0)
    );

    given(objectMapper.readTree(event.getPayload()))
        .willReturn(new ObjectMapper().readTree(event.getPayload()));
    publisher.publish(event);

    verify(dispatchService).dispatch(44L);
  }
}
