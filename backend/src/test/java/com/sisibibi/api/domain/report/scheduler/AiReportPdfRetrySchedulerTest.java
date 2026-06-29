package com.sisibibi.api.domain.report.scheduler;

import com.sisibibi.api.domain.report.entity.AiReportNotificationStatus;
import com.sisibibi.api.domain.report.entity.AiReportPdfExport;
import com.sisibibi.api.domain.report.entity.AiReportPdfStatus;
import com.sisibibi.api.domain.report.repository.AiReportPdfExportRepository;
import com.sisibibi.api.domain.report.service.AiReportPdfGenerationService;
import com.sisibibi.api.domain.report.service.AiReportPdfProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiReportPdfRetrySchedulerTest {

    @Mock private AiReportPdfExportRepository exportRepository;
    @Mock private AiReportPdfGenerationService generationService;

    private AiReportPdfProperties properties;
    private AiReportPdfRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new AiReportPdfProperties();
        scheduler = new AiReportPdfRetryScheduler(exportRepository, generationService, properties);
    }

    @Test
    void retryFailedNotifications_callsRetryNotificationForCandidates() {
        AiReportPdfExport notifFailedExport = exportWithId(77L);
        given(exportRepository.findNotificationRetryCandidates(
                any(AiReportPdfStatus.class), any(AiReportNotificationStatus.class),
                any(LocalDateTime.class), anyInt(), any(Pageable.class)
        )).willReturn(List.of(notifFailedExport));

        scheduler.retryFailedNotifications();

        verify(generationService).retryNotification(77L);
    }

    @Test
    void retryFailedNotifications_doesNotCallGenerate() {
        given(exportRepository.findNotificationRetryCandidates(
                any(AiReportPdfStatus.class), any(AiReportNotificationStatus.class),
                any(LocalDateTime.class), anyInt(), any(Pageable.class)
        )).willReturn(List.of());

        scheduler.retryFailedNotifications();

        verify(generationService, never()).generate(any());
    }

    @Test
    void disabledScheduler_skipsWork() {
        properties.setEnabled(false);

        scheduler.retryFailedNotifications();

        verify(exportRepository, never()).findNotificationRetryCandidates(
                any(AiReportPdfStatus.class), any(AiReportNotificationStatus.class),
                any(LocalDateTime.class), anyInt(), any(Pageable.class)
        );
        verify(generationService, never()).retryNotification(any());
    }

    private AiReportPdfExport exportWithId(long id) {
        AiReportPdfExport export = AiReportPdfExport.notStarted(10L, 1L, 5L);
        try {
            var field = AiReportPdfExport.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(export, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return export;
    }
}
