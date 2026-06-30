package com.sisibibi.api.domain.payment.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sisibibi.api.domain.payment.entity.CustomAiReportRequest;
import com.sisibibi.api.domain.payment.repository.CustomAiReportRequestRepository;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.domain.report.entity.AiReportCustomPrompt;
import com.sisibibi.api.domain.report.queue.AiReportQueueMessage;
import com.sisibibi.api.domain.report.queue.AiReportQueuePublisher;
import com.sisibibi.api.domain.report.service.AiReportGenerationType;
import com.sisibibi.api.domain.report.service.AiReportPdfPersistenceService;
import com.sisibibi.api.domain.report.service.AiReportPersistenceService;
import com.sisibibi.api.domain.report.service.AiReportRequestResult;
import com.sisibibi.api.global.outbox.OutboxEvent;
import com.sisibibi.api.global.outbox.OutboxEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomAiReportGenerationOutboxPublisherTest {

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private CustomAiReportRequestRepository customAiReportRequestRepository;

  @Mock
  private AiReportPersistenceService aiReportPersistenceService;

  @Mock
  private AiReportQueuePublisher aiReportQueuePublisher;

  @Mock
  private AiReportPdfPersistenceService aiReportPdfPersistenceService;

  @InjectMocks
  private CustomAiReportGenerationOutboxPublisher publisher;

  @Test
  void publish_createsRequesterPdfExportBeforeQueueingCustomReport() throws Exception {
    CustomAiReportRequest customRequest = CustomAiReportRequest.pendingPayment(
        10L,
        7L,
        List.of(new AiReportCustomPrompt(7L, "custom 1", "minority view"))
    );
    ReflectionTestUtils.setField(customRequest, "id", 44L);
    AiReportRes response = new AiReportRes(
        55L,
        10L,
        "REQUESTED",
        null,
        List.of(),
        null,
        null,
        null,
        null,
        null,
        null
    );
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
    given(customAiReportRequestRepository.findById(44L)).willReturn(Optional.of(customRequest));
    given(aiReportPersistenceService.requestCustomGeneration(eq(10L), eq(7L), any()))
        .willReturn(AiReportRequestResult.publish(response, AiReportGenerationType.CUSTOM_ONLY));

    publisher.publish(event);

    verify(aiReportPdfPersistenceService).createIfMissing(55L, 10L, 7L);
    verify(aiReportQueuePublisher).publish(any(AiReportQueueMessage.class));
  }
}
