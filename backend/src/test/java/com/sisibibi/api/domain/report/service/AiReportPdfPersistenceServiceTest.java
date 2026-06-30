package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.entity.AiReportNotificationStatus;
import com.sisibibi.api.domain.report.entity.AiReportPdfExport;
import com.sisibibi.api.domain.report.entity.AiReportPdfStatus;
import com.sisibibi.api.domain.report.entity.AiReportPdfType;
import com.sisibibi.api.domain.report.repository.AiReportPdfExportRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiReportPdfPersistenceServiceTest {

    @InjectMocks
    private AiReportPdfPersistenceService persistenceService;

    @Mock
    private AiReportPdfExportRepository exportRepository;

    @Test
    void prepareGeneration_marksNotStartedExportGenerating() {
        AiReportPdfExport export = AiReportPdfExport.notStarted(10L, 20L, 7L, AiReportPdfType.BASE);
        ReflectionTestUtils.setField(export, "id", 1L);
        given(exportRepository.findByIdForUpdate(1L)).willReturn(Optional.of(export));

        AiReportPdfExport result = persistenceService.prepareGeneration(1L);

        assertThat(result.getPdfStatus()).isEqualTo(AiReportPdfStatus.GENERATING);
    }

    @Test
    void prepareGeneration_rejectsMissingExport() {
        given(exportRepository.findByIdForUpdate(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> persistenceService.prepareGeneration(999L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.AI_REPORT_PDF_EXPORT_NOT_FOUND.getMessage());
    }

    @Test
    void completePdf_marksExportReady() {
        AiReportPdfExport export = AiReportPdfExport.notStarted(10L, 20L, 7L, AiReportPdfType.BASE);
        ReflectionTestUtils.setField(export, "id", 1L);
        export.markGenerating(null);
        given(exportRepository.findByIdForUpdate(1L)).willReturn(Optional.of(export));

        persistenceService.completePdf(1L, "ai-reports/20/10/7.pdf");

        assertThat(export.getPdfStatus()).isEqualTo(AiReportPdfStatus.READY);
        assertThat(export.getPdfObjectKey()).isEqualTo("ai-reports/20/10/7.pdf");
    }

    @Test
    void failPdf_marksFailedAndIncrementsRetryCount() {
        AiReportPdfExport export = AiReportPdfExport.notStarted(10L, 20L, 7L, AiReportPdfType.BASE);
        ReflectionTestUtils.setField(export, "id", 1L);
        export.markGenerating(null);
        given(exportRepository.findByIdForUpdate(1L)).willReturn(Optional.of(export));

        persistenceService.failPdf(1L, "render failed");

        assertThat(export.getPdfStatus()).isEqualTo(AiReportPdfStatus.FAILED);
        assertThat(export.getPdfRetryCount()).isEqualTo(1);
    }

    @Test
    void markNotificationFailed_keepsPdfReady() {
        AiReportPdfExport export = AiReportPdfExport.notStarted(10L, 20L, 7L, AiReportPdfType.BASE);
        ReflectionTestUtils.setField(export, "id", 1L);
        export.markGenerating(null);
        export.markPdfReady("ai-reports/20/10/7.pdf", null);
        given(exportRepository.findByIdForUpdate(1L)).willReturn(Optional.of(export));

        persistenceService.markNotificationFailed(1L, "smtp failed");

        assertThat(export.getPdfStatus()).isEqualTo(AiReportPdfStatus.READY);
        assertThat(export.getNotificationStatus()).isEqualTo(AiReportNotificationStatus.FAILED);
    }

    @Test
    void createIfMissing_usesPdfTypeAsPartOfIdentity() {
        given(exportRepository.findByAiReportIdAndRequestedByUserIdAndPdfTypeForUpdate(
                10L, 7L, AiReportPdfType.CUSTOM
        )).willReturn(Optional.empty());

        persistenceService.createIfMissing(10L, 20L, 7L, AiReportPdfType.CUSTOM);

        verify(exportRepository).save(org.mockito.ArgumentMatchers.argThat(export ->
                export.getPdfType() == AiReportPdfType.CUSTOM
        ));
    }
}
