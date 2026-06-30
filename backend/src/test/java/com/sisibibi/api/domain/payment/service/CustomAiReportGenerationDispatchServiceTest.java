package com.sisibibi.api.domain.payment.service;

import com.sisibibi.api.domain.payment.entity.CustomAiReportRequest;
import com.sisibibi.api.domain.payment.entity.CustomAiReportRequestStatus;
import com.sisibibi.api.domain.payment.repository.CustomAiReportRequestRepository;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.domain.report.entity.AiReportCustomPrompt;
import com.sisibibi.api.domain.report.entity.AiReportPdfType;
import com.sisibibi.api.domain.report.queue.AiReportQueueMessage;
import com.sisibibi.api.domain.report.queue.AiReportQueuePublisher;
import com.sisibibi.api.domain.report.service.AiReportGenerationType;
import com.sisibibi.api.domain.report.service.AiReportPdfPersistenceService;
import com.sisibibi.api.domain.report.service.AiReportPersistenceService;
import com.sisibibi.api.domain.report.service.AiReportRequestResult;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomAiReportGenerationDispatchServiceTest {

  @Mock
  private CustomAiReportRequestRepository customAiReportRequestRepository;

  @Mock
  private AiReportPersistenceService aiReportPersistenceService;

  @Mock
  private AiReportQueuePublisher aiReportQueuePublisher;

  @Mock
  private AiReportPdfPersistenceService aiReportPdfPersistenceService;

  private CustomAiReportGenerationDispatchService dispatchService;

  @BeforeEach
  void setUp() {
    dispatchService = new CustomAiReportGenerationDispatchService(
        customAiReportRequestRepository,
        aiReportPersistenceService,
        aiReportQueuePublisher,
        aiReportPdfPersistenceService
    );
  }

  @Test
  void dispatch_createsRequesterPdfExportBeforeQueueingCustomReport() {
    CustomAiReportRequest customRequest = paidCustomRequest();
    AiReportRes response = requestedReportResponse();

    given(customAiReportRequestRepository.findById(44L)).willReturn(Optional.of(customRequest));
    given(aiReportPersistenceService.requestCustomGeneration(eq(10L), eq(7L), any()))
        .willReturn(AiReportRequestResult.publish(response, AiReportGenerationType.CUSTOM_ONLY));

    dispatchService.dispatch(44L);

    ArgumentCaptor<AiReportQueueMessage> messageCaptor =
        ArgumentCaptor.forClass(AiReportQueueMessage.class);
    verify(aiReportPdfPersistenceService).createIfMissing(55L, 10L, 7L, AiReportPdfType.CUSTOM);
    verify(aiReportQueuePublisher).publish(messageCaptor.capture());
    verify(aiReportPersistenceService).markQueued(55L);
    verify(customAiReportRequestRepository).save(customRequest);
    assertThat(customRequest.getStatus()).isEqualTo(CustomAiReportRequestStatus.QUEUED);
    assertThat(messageCaptor.getValue().generationType()).isEqualTo(AiReportGenerationType.CUSTOM_ONLY);
  }

  @Test
  void dispatch_marksPublishFailed_whenQueuePublishFails() {
    CustomAiReportRequest customRequest = paidCustomRequest();
    AiReportRes response = requestedReportResponse();
    RuntimeException publishException = new RuntimeException("queue failed");

    given(customAiReportRequestRepository.findById(44L)).willReturn(Optional.of(customRequest));
    given(aiReportPersistenceService.requestCustomGeneration(eq(10L), eq(7L), any()))
        .willReturn(AiReportRequestResult.publish(response, AiReportGenerationType.CUSTOM_ONLY));
    willThrow(publishException).given(aiReportQueuePublisher).publish(any(AiReportQueueMessage.class));

    assertThatThrownBy(() -> dispatchService.dispatch(44L))
        .isSameAs(publishException);

    verify(aiReportPersistenceService).markPublishFailed(
        55L,
        ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.name(),
        ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.getMessage()
    );
    verify(customAiReportRequestRepository).save(customRequest);
    assertThat(customRequest.getStatus()).isEqualTo(CustomAiReportRequestStatus.PUBLISH_FAILED);
  }

  @Test
  void dispatch_publishesCustomMessage_whenBaseReportIsStillGenerating() {
    CustomAiReportRequest customRequest = paidCustomRequest();
    AiReportRes response = inProgressReportResponse();

    given(customAiReportRequestRepository.findById(44L)).willReturn(Optional.of(customRequest));
    given(aiReportPersistenceService.requestCustomGeneration(eq(10L), eq(7L), any()))
        .willReturn(AiReportRequestResult.publish(response, AiReportGenerationType.CUSTOM_ONLY));

    dispatchService.dispatch(44L);

    verify(aiReportPdfPersistenceService).createIfMissing(55L, 10L, 7L, AiReportPdfType.CUSTOM);
    verify(aiReportQueuePublisher).publish(any(AiReportQueueMessage.class));
    verify(aiReportPersistenceService, never()).markQueued(55L);
    assertThat(customRequest.getStatus()).isEqualTo(CustomAiReportRequestStatus.QUEUED);
  }

  @Test
  void dispatch_doesNotMarkBaseReportPublishFailed_whenCustomPublishFailsBeforeBaseCompletion() {
    CustomAiReportRequest customRequest = paidCustomRequest();
    AiReportRes response = inProgressReportResponse();
    RuntimeException publishException = new RuntimeException("queue failed");

    given(customAiReportRequestRepository.findById(44L)).willReturn(Optional.of(customRequest));
    given(aiReportPersistenceService.requestCustomGeneration(eq(10L), eq(7L), any()))
        .willReturn(AiReportRequestResult.publish(response, AiReportGenerationType.CUSTOM_ONLY));
    willThrow(publishException).given(aiReportQueuePublisher).publish(any(AiReportQueueMessage.class));

    assertThatThrownBy(() -> dispatchService.dispatch(44L))
        .isSameAs(publishException);

    verify(aiReportPersistenceService, never()).markPublishFailed(any(), any(), any());
    verify(customAiReportRequestRepository).save(customRequest);
    assertThat(customRequest.getStatus()).isEqualTo(CustomAiReportRequestStatus.PUBLISH_FAILED);
  }

  private CustomAiReportRequest paidCustomRequest() {
    CustomAiReportRequest customRequest = CustomAiReportRequest.pendingPayment(
        10L,
        7L,
        List.of(new AiReportCustomPrompt(7L, "custom 1", "minority view"))
    );
    ReflectionTestUtils.setField(customRequest, "id", 44L);
    customRequest.markPaid();
    return customRequest;
  }

  private AiReportRes requestedReportResponse() {
    return new AiReportRes(
        55L,
        10L,
        "REQUESTED",
        "core",
        List.of("issue"),
        "summary",
        "common",
        "opinion",
        null,
        null,
        null
    );
  }

  private AiReportRes inProgressReportResponse() {
    return new AiReportRes(
        55L,
        10L,
        "PROCESSING",
        null,
        List.of(),
        null,
        null,
        null,
        null,
        null,
        null
    );
  }
}
