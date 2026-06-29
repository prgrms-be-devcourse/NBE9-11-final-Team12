package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes;
import com.sisibibi.api.domain.report.entity.AiReport;
import com.sisibibi.api.domain.report.entity.AiReportCustomPrompt;
import com.sisibibi.api.domain.report.entity.AiReportStatus;
import com.sisibibi.api.domain.report.queue.AiReportQueueMessage;
import com.sisibibi.api.domain.report.queue.AiReportQueueProperties;
import com.sisibibi.api.domain.report.queue.AiReportQueuePublisher;
import com.sisibibi.api.domain.report.repository.AiReportRepository;
import com.sisibibi.api.domain.report.worker.AiReportWorkerEc2Service;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AiReportPublishRetryServiceTest {

    @Mock
    private AiReportRepository aiReportRepository;

    @Mock
    private AiReportQueuePublisher aiReportQueuePublisher;

    @Mock
    private AiReportPersistenceService aiReportPersistenceService;

    @Mock
    private AiReportWorkerEc2Service aiReportWorkerEc2Service;

    private AiReportQueueProperties properties;
    private AiReportPublishRetryService service;

    @BeforeEach
    void setUp() {
        properties = new AiReportQueueProperties();
        properties.getRetry().setStaleThreshold(Duration.ofMinutes(1));
        properties.getRetry().setBatchSize(10);
        properties.getRetry().setMaxRetryCount(3);
        service = new AiReportPublishRetryService(
                aiReportRepository,
                aiReportQueuePublisher,
                aiReportPersistenceService,
                properties,
                aiReportWorkerEc2Service
        );
    }

    @Test
    void republishStaleRequests_publishesRequestedCandidateAndMarksQueued() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 25, 12, 0);
        AiReport report = reportWithId(AiReport.requested(10L), 55L);
        given(aiReportRepository.findPublishRetryCandidates(
                List.of(AiReportStatus.REQUESTED, AiReportStatus.PUBLISH_FAILED),
                now.minusMinutes(1),
                3,
                PageRequest.of(0, 10)
        )).willReturn(List.of(report));

        int successCount = service.republishStaleRequests(now);

        ArgumentCaptor<AiReportQueueMessage> messageCaptor = ArgumentCaptor.forClass(AiReportQueueMessage.class);
        verify(aiReportQueuePublisher).publish(messageCaptor.capture());
        assertThat(successCount).isEqualTo(1);
        assertThat(messageCaptor.getValue().reportId()).isEqualTo(55L);
        assertThat(messageCaptor.getValue().roomId()).isEqualTo(10L);
        assertThat(messageCaptor.getValue().generationType()).isEqualTo(AiReportGenerationType.BASE_ONLY);
        verify(aiReportPersistenceService).markQueued(55L);
        verify(aiReportWorkerEc2Service).startWorkerIfEnabled();
    }

    @Test
    void republishStaleRequests_restoresCustomOnlyType_whenBaseReportAlreadyExists() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 25, 12, 0);
        AiReport report = reportWithId(AiReport.requested(10L), 56L);
        report.complete(new AiReportGenerateRes(
                "core",
                List.of("issue"),
                "summary",
                "common",
                "opinion"
        ));
        report.requestCustomReports(List.of(new AiReportCustomPrompt(7L, "custom 1", "minority view")));
        given(aiReportRepository.findPublishRetryCandidates(
                List.of(AiReportStatus.REQUESTED, AiReportStatus.PUBLISH_FAILED),
                now.minusMinutes(1),
                3,
                PageRequest.of(0, 10)
        )).willReturn(List.of(report));

        service.republishStaleRequests(now);

        ArgumentCaptor<AiReportQueueMessage> messageCaptor = ArgumentCaptor.forClass(AiReportQueueMessage.class);
        verify(aiReportQueuePublisher).publish(messageCaptor.capture());
        assertThat(messageCaptor.getValue().generationType()).isEqualTo(AiReportGenerationType.CUSTOM_ONLY);
        verify(aiReportPersistenceService).markQueued(56L);
        verify(aiReportWorkerEc2Service).startWorkerIfEnabled();
    }

    @Test
    void republishStaleRequests_marksPublishFailed_whenPublishFails() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 25, 12, 0);
        AiReport report = reportWithId(AiReport.requested(
                10L,
                List.of(new AiReportCustomPrompt(7L, "custom 1", "minority view"))
        ), 57L);
        given(aiReportRepository.findPublishRetryCandidates(
                List.of(AiReportStatus.REQUESTED, AiReportStatus.PUBLISH_FAILED),
                now.minusMinutes(1),
                3,
                PageRequest.of(0, 10)
        )).willReturn(List.of(report));
        doThrow(new IllegalStateException("sqs unavailable"))
                .when(aiReportQueuePublisher)
                .publish(AiReportQueueMessage.of(57L, 10L, AiReportGenerationType.BASE_WITH_CUSTOM));

        int successCount = service.republishStaleRequests(now);

        assertThat(successCount).isZero();
        verify(aiReportPersistenceService).markPublishFailed(
                57L,
                ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.name(),
                ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.getMessage()
        );
        verifyNoInteractions(aiReportWorkerEc2Service);
    }

    @Test
    void republishStaleRequests_doesNothing_whenThereAreNoCandidates() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 25, 12, 0);
        given(aiReportRepository.findPublishRetryCandidates(
                List.of(AiReportStatus.REQUESTED, AiReportStatus.PUBLISH_FAILED),
                now.minusMinutes(1),
                3,
                PageRequest.of(0, 10)
        )).willReturn(List.of());

        int successCount = service.republishStaleRequests(now);

        assertThat(successCount).isZero();
        verifyNoInteractions(aiReportQueuePublisher, aiReportPersistenceService);
    }

    private AiReport reportWithId(AiReport report, Long id) {
        ReflectionTestUtils.setField(report, "id", id);
        return report;
    }
}
