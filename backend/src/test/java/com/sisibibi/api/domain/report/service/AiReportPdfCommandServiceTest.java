package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.dto.response.AiReportPdfDownloadUrlRes;
import com.sisibibi.api.domain.report.dto.response.AiReportPdfStatusRes;
import com.sisibibi.api.domain.report.dto.response.AiReportStatusRes;
import com.sisibibi.api.domain.report.entity.AiReport;
import com.sisibibi.api.domain.report.entity.AiReportPdfExport;
import com.sisibibi.api.domain.report.entity.AiReportStatus;
import com.sisibibi.api.domain.report.outbox.AiReportPdfGenerationOutboxWriter;
import com.sisibibi.api.domain.report.repository.AiReportPdfExportRepository;
import com.sisibibi.api.domain.report.repository.AiReportRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiReportPdfCommandServiceTest {

    @InjectMocks
    private AiReportPdfCommandService commandService;

    @Mock
    private AiReportRepository aiReportRepository;

    @Mock
    private AiReportPdfExportRepository exportRepository;

    @Mock
    private AiReportPdfPersistenceService persistenceService;

    @Mock
    private AiReportPdfStorage storage;

    @Mock
    private AiReportPdfGenerationOutboxWriter outboxWriter;

    @Test
    void getStatus_returnsNotStartedPdf_whenExportMissing() {
        AiReport report = completedReport(10L, 1L);
        given(aiReportRepository.findByRoomId(1L)).willReturn(Optional.of(report));
        given(exportRepository.findByAiReportIdAndRequestedByUserId(10L, 7L)).willReturn(Optional.empty());

        AiReportStatusRes result = commandService.getStatus(1L, 7L);

        assertThat(result.pdf().pdfStatus()).isEqualTo("NOT_STARTED");
        assertThat(result.pdf().downloadAvailable()).isFalse();
    }

    @Test
    void requestPdf_createsExportAndRecordsOutboxEvent_whenReportCompleted() {
        AiReport report = completedReport(10L, 1L);
        AiReportPdfExport export = AiReportPdfExport.notStarted(10L, 1L, 7L);
        ReflectionTestUtils.setField(export, "id", 99L);
        given(aiReportRepository.findByRoomId(1L)).willReturn(Optional.of(report));
        given(persistenceService.createIfMissing(10L, 1L, 7L)).willReturn(export);

        AiReportPdfStatusRes result = commandService.requestPdf(1L, 7L);

        assertThat(result.pdfStatus()).isEqualTo("NOT_STARTED");
        verify(outboxWriter).record(eq(99L), any(LocalDateTime.class));
    }

    @Test
    void downloadUrl_rejectsMissingExport() {
        given(exportRepository.findByRoomIdAndRequestedByUserId(1L, 7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commandService.createDownloadUrl(1L, 7L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.AI_REPORT_PDF_EXPORT_NOT_FOUND.getMessage());
    }

    @Test
    void downloadUrl_rejectsNotReadyExport() {
        AiReportPdfExport export = AiReportPdfExport.notStarted(10L, 1L, 7L);
        given(exportRepository.findByRoomIdAndRequestedByUserId(1L, 7L)).willReturn(Optional.of(export));

        assertThatThrownBy(() -> commandService.createDownloadUrl(1L, 7L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.AI_REPORT_PDF_NOT_READY.getMessage());

        verify(storage, never()).createDownloadUrl(any(), any());
    }

    @Test
    void downloadUrl_returnsPresignedUrl_forOwnerReadyExport() {
        AiReportPdfExport export = AiReportPdfExport.notStarted(10L, 1L, 7L);
        export.markGenerating(null);
        export.markPdfReady("ai-reports/1/10/7.pdf", null);
        given(exportRepository.findByRoomIdAndRequestedByUserId(1L, 7L)).willReturn(Optional.of(export));
        given(storage.createDownloadUrl("ai-reports/1/10/7.pdf", "ai-report-room-1.pdf"))
                .willReturn(new AiReportPdfStorage.DownloadUrl("https://s3.example.com/presigned", Instant.parse("2026-06-28T12:11:10Z")));

        AiReportPdfDownloadUrlRes result = commandService.createDownloadUrl(1L, 7L);

        assertThat(result.downloadUrl()).isEqualTo("https://s3.example.com/presigned");
        assertThat(result.expiresAt()).isEqualTo(Instant.parse("2026-06-28T12:11:10Z"));
    }

    private AiReport completedReport(Long reportId, Long roomId) {
        AiReport report = AiReport.requested(roomId);
        ReflectionTestUtils.setField(report, "id", reportId);
        ReflectionTestUtils.setField(report, "status", AiReportStatus.COMPLETED);
        return report;
    }
}
