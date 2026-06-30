package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.entity.AiReportCustomReport;
import com.sisibibi.api.domain.report.entity.AiReportPdfExport;
import com.sisibibi.api.domain.report.entity.AiReportPdfType;
import com.sisibibi.api.domain.report.notification.AiReportNotificationProperties;
import com.sisibibi.api.domain.report.outbox.AiReportPdfNotificationOutboxWriter;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiReportPdfGenerationServiceTest {

    @Mock private AiReportPdfPersistenceService persistenceService;
    @Mock private AiReportPdfDataCollector dataCollector;
    @Mock private AiReportPdfRenderer renderer;
    @Mock private AiReportPdfStorage storage;
    @Mock private AiReportNotificationSender notificationSender;
    @Mock private AiReportPdfNotificationOutboxWriter notificationOutboxWriter;

    private AiReportPdfGenerationService service;

    private static final long EXPORT_ID = 1L;

    @BeforeEach
    void setUp() {
        AiReportNotificationProperties notificationProperties = new AiReportNotificationProperties();
        notificationProperties.setHomepageUrl("http://localhost:3000");
        service = new AiReportPdfGenerationService(
                persistenceService, dataCollector, renderer, storage, notificationSender,
                notificationProperties, notificationOutboxWriter
        );
    }

    @Test
    void generate_success_uploadsPdfMarksReadyAndSendsEmail() {
        AiReportPdfExport export = AiReportPdfExport.notStarted(10L, 1L, 5L);
        AiReportPdfModel model = sampleModel();

        given(persistenceService.prepareGeneration(EXPORT_ID)).willReturn(export);
        given(dataCollector.collect(export)).willReturn(model);
        given(renderer.render(model)).willReturn(new byte[]{1, 2, 3});
        given(storage.upload(model.roomId(), model.reportId(), export.getRequestedByUserId(), AiReportPdfType.BASE, new byte[]{1, 2, 3}))
                .willReturn("ai-reports/1/10/5-base.pdf");

        service.generate(EXPORT_ID);

        verify(persistenceService).completePdf(EXPORT_ID, "ai-reports/1/10/5-base.pdf");
        verify(notificationSender).sendPdfReady(any(AiReportPdfReadyCommand.class));
        verify(persistenceService).markNotificationSent(EXPORT_ID);
        verify(persistenceService, never()).failPdf(any(), any());
    }

    @Test
    void generate_pdfFailureRetriesThreeTimesAndThrows() {
        AiReportPdfExport export = AiReportPdfExport.notStarted(10L, 1L, 5L);

        given(persistenceService.prepareGeneration(EXPORT_ID)).willReturn(export);
        given(dataCollector.collect(export)).willThrow(new RuntimeException("render failed"));

        assertThatThrownBy(() -> service.generate(EXPORT_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("render failed");

        verify(dataCollector, times(3)).collect(export);
        verify(persistenceService).failPdf(eq(EXPORT_ID), any());
        verify(notificationSender, never()).sendPdfReady(any());
        verify(persistenceService, never()).completePdf(any(), any());
    }

    @Test
    void generate_notificationFailureKeepsPdfReadyAndRecordsOutboxEvent() {
        AiReportPdfExport export = AiReportPdfExport.notStarted(10L, 1L, 5L);
        AiReportPdfModel model = sampleModel();

        given(persistenceService.prepareGeneration(EXPORT_ID)).willReturn(export);
        given(dataCollector.collect(export)).willReturn(model);
        given(renderer.render(model)).willReturn(new byte[]{1});
        given(storage.upload(any(), any(), any(), any(), any())).willReturn("key");
        willThrow(new RuntimeException("smtp failed")).given(notificationSender).sendPdfReady(any());

        service.generate(EXPORT_ID);

        verify(persistenceService).completePdf(EXPORT_ID, "key");
        verify(persistenceService).markNotificationFailed(eq(EXPORT_ID), any());
        verify(notificationOutboxWriter).record(eq(EXPORT_ID), any(LocalDateTime.class));
        verify(persistenceService, never()).failPdf(any(), any());
    }

    @Test
    void retryNotification_success_marksNotificationSent() {
        AiReportPdfExport export = AiReportPdfExport.notStarted(10L, 1L, 5L);
        AiReportPdfModel model = sampleModel();

        given(persistenceService.loadExport(EXPORT_ID)).willReturn(export);
        given(dataCollector.collect(export)).willReturn(model);

        service.retryNotification(EXPORT_ID);

        verify(notificationSender).sendPdfReady(any(AiReportPdfReadyCommand.class));
        verify(persistenceService).markNotificationSent(EXPORT_ID);
    }

    @Test
    void retryNotification_emailFailure_marksFailedAndThrows() {
        AiReportPdfExport export = AiReportPdfExport.notStarted(10L, 1L, 5L);
        AiReportPdfModel model = sampleModel();

        given(persistenceService.loadExport(EXPORT_ID)).willReturn(export);
        given(dataCollector.collect(export)).willReturn(model);
        willThrow(new RuntimeException("smtp error")).given(notificationSender).sendPdfReady(any());

        assertThatThrownBy(() -> service.retryNotification(EXPORT_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("smtp error");

        verify(persistenceService).markNotificationFailed(eq(EXPORT_ID), any());
        verify(persistenceService, never()).markNotificationSent(any());
    }

    private AiReportPdfModel sampleModel() {
        return new AiReportPdfModel(
                1L, 10L, 5L,
                "AI 토론방", "토픽 제목", "토픽 설명",
                "user@test.com", "테스터",
                10, 15L, 20L, 8L, 7L,
                "핵심 한 줄",
                List.of("쟁점1"),
                "공통 의견",
                "AI 요약",
                "AI 의견",
                List.of(new AiReportPdfModel.TopOpinion(1L, 5L, "유저A", SpeechStance.PRO, "찬성 의견", 3L, LocalDateTime.now())),
                List.of(new AiReportPdfModel.TopOpinion(2L, 6L, "유저B", SpeechStance.CON, "반대 의견", 2L, LocalDateTime.now())),
                List.of(new AiReportCustomReport("경제 분석", "경제 측면에서 분석해줘", "경제 영향", "AI 도입으로 생산성이 향상됩니다.")),
                LocalDateTime.now()
        );
    }
}
