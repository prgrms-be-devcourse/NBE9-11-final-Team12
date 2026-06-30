package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.entity.AiReportPdfExport;
import com.sisibibi.api.domain.report.entity.AiReportPdfType;
import com.sisibibi.api.domain.report.repository.AiReportPdfExportRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AiReportPdfPersistenceService {

    private final AiReportPdfExportRepository exportRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public AiReportPdfExport createIfMissing(Long aiReportId, Long roomId, Long userId) {
        return createIfMissing(aiReportId, roomId, userId, AiReportPdfType.BASE);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AiReportPdfExport createIfMissing(Long aiReportId, Long roomId, Long userId, AiReportPdfType pdfType) {
        AiReportPdfType actualType = pdfType == null ? AiReportPdfType.BASE : pdfType;
        return exportRepository.findByAiReportIdAndRequestedByUserIdAndPdfTypeForUpdate(aiReportId, userId, actualType)
                .orElseGet(() -> exportRepository.save(AiReportPdfExport.notStarted(aiReportId, roomId, userId, actualType)));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AiReportPdfExport prepareGeneration(Long exportId) {
        AiReportPdfExport export = findForUpdate(exportId);
        export.markGenerating(LocalDateTime.now());
        return export;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void completePdf(Long exportId, String objectKey) {
        findForUpdate(exportId).markPdfReady(objectKey, LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void failPdf(Long exportId, String errorMessage) {
        findForUpdate(exportId).markPdfFailed(errorMessage, LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void markNotificationSent(Long exportId) {
        findForUpdate(exportId).markNotificationSent(LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void markNotificationFailed(Long exportId, String errorMessage) {
        findForUpdate(exportId).markNotificationFailed(errorMessage, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public AiReportPdfExport loadExport(Long exportId) {
        return exportRepository.findById(exportId)
                .orElseThrow(() -> new CustomException(ErrorCode.AI_REPORT_PDF_EXPORT_NOT_FOUND));
    }

    private AiReportPdfExport findForUpdate(Long exportId) {
        return exportRepository.findByIdForUpdate(exportId)
                .orElseThrow(() -> new CustomException(ErrorCode.AI_REPORT_PDF_EXPORT_NOT_FOUND));
    }
}
