package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.dto.response.AiReportPdfDownloadUrlRes;
import com.sisibibi.api.domain.report.dto.response.AiReportPdfStatusRes;
import com.sisibibi.api.domain.report.dto.response.AiReportStatusRes;
import com.sisibibi.api.domain.report.entity.AiReport;
import com.sisibibi.api.domain.report.entity.AiReportPdfExport;
import com.sisibibi.api.domain.report.entity.AiReportPdfStatus;
import com.sisibibi.api.domain.report.entity.AiReportStatus;
import com.sisibibi.api.domain.report.outbox.AiReportPdfGenerationOutboxWriter;
import com.sisibibi.api.domain.report.repository.AiReportPdfExportRepository;
import com.sisibibi.api.domain.report.repository.AiReportRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiReportPdfCommandService {

    private final AiReportRepository aiReportRepository;
    private final AiReportPdfExportRepository exportRepository;
    private final AiReportPdfPersistenceService persistenceService;
    private final AiReportPdfStorage storage;
    private final AiReportPdfGenerationOutboxWriter outboxWriter;

    public AiReportStatusRes getStatus(Long roomId, Long userId) {
        AiReport report = aiReportRepository.findByRoomId(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.AI_REPORT_NOT_FOUND));
        AiReportPdfStatusRes pdf = exportRepository.findByAiReportIdAndRequestedByUserId(report.getId(), userId)
                .map(AiReportPdfStatusRes::from)
                .orElse(AiReportPdfStatusRes.notStarted());
        return new AiReportStatusRes(roomId, report.getId(), report.getStatus().name(), pdf, report.getRequestedAt(), report.getCompletedAt());
    }

    @Transactional
    public AiReportPdfStatusRes requestPdf(Long roomId, Long userId) {
        AiReport report = aiReportRepository.findByRoomId(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.AI_REPORT_NOT_FOUND));
        if (report.getStatus() != AiReportStatus.COMPLETED) {
            throw new CustomException(ErrorCode.AI_REPORT_PDF_NOT_READY);
        }
        AiReportPdfExport export = persistenceService.createIfMissing(report.getId(), roomId, userId);
        if (export.shouldStartGeneration()) {
            outboxWriter.record(export.getId(), LocalDateTime.now());
        }
        return AiReportPdfStatusRes.from(export);
    }

    public AiReportPdfDownloadUrlRes createDownloadUrl(Long roomId, Long userId) {
        AiReportPdfExport export = exportRepository.findByRoomIdAndRequestedByUserId(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AI_REPORT_PDF_EXPORT_NOT_FOUND));
        if (export.getPdfStatus() != AiReportPdfStatus.READY || export.getPdfObjectKey() == null) {
            throw new CustomException(ErrorCode.AI_REPORT_PDF_NOT_READY);
        }
        AiReportPdfStorage.DownloadUrl downloadUrl = storage.createDownloadUrl(
                export.getPdfObjectKey(),
                "ai-report-room-" + roomId + ".pdf"
        );
        return new AiReportPdfDownloadUrlRes(downloadUrl.url(), downloadUrl.expiresAt());
    }
}
