package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.dto.response.AiReportPdfDownloadUrlRes;
import com.sisibibi.api.domain.report.dto.response.AiReportPdfStatusRes;
import com.sisibibi.api.domain.report.dto.response.AiReportStatusRes;
import com.sisibibi.api.domain.report.entity.AiReport;
import com.sisibibi.api.domain.report.entity.AiReportPdfExport;
import com.sisibibi.api.domain.report.entity.AiReportPdfStatus;
import com.sisibibi.api.domain.report.entity.AiReportPdfType;
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
        AiReportPdfStatusRes pdf = exportRepository.findByAiReportIdAndRequestedByUserIdAndPdfType(
                        report.getId(),
                        userId,
                        AiReportPdfType.BASE
                )
                .map(AiReportPdfStatusRes::from)
                .orElse(AiReportPdfStatusRes.notStarted());
        return new AiReportStatusRes(roomId, report.getId(), report.getStatus().name(), pdf, report.getRequestedAt(), report.getCompletedAt());
    }

    @Transactional
    public AiReportPdfStatusRes requestPdf(Long roomId, Long userId) {
        return requestPdf(roomId, userId, AiReportPdfType.BASE);
    }

    @Transactional
    public AiReportPdfStatusRes requestPdf(Long roomId, Long userId, AiReportPdfType pdfType) {
        AiReport report = aiReportRepository.findByRoomId(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.AI_REPORT_NOT_FOUND));
        if (report.getStatus() != AiReportStatus.COMPLETED) {
            throw new CustomException(ErrorCode.AI_REPORT_PDF_NOT_READY);
        }
        AiReportPdfType actualType = normalize(pdfType);
        if (actualType == AiReportPdfType.CUSTOM && !hasVisibleCustomReport(report, userId)) {
            throw new CustomException(ErrorCode.AI_REPORT_PDF_NOT_READY);
        }
        AiReportPdfExport export = persistenceService.createIfMissing(report.getId(), roomId, userId, actualType);
        if (export.shouldStartGeneration()) {
            outboxWriter.record(export.getId(), LocalDateTime.now());
        }
        return AiReportPdfStatusRes.from(export);
    }

    public AiReportPdfDownloadUrlRes createDownloadUrl(Long roomId, Long userId) {
        return createDownloadUrl(roomId, userId, AiReportPdfType.BASE);
    }

    public AiReportPdfDownloadUrlRes createDownloadUrl(Long roomId, Long userId, AiReportPdfType pdfType) {
        AiReportPdfType actualType = normalize(pdfType);
        AiReportPdfExport export = exportRepository.findByRoomIdAndRequestedByUserIdAndPdfType(roomId, userId, actualType)
                .orElseThrow(() -> new CustomException(ErrorCode.AI_REPORT_PDF_EXPORT_NOT_FOUND));
        if (export.getPdfStatus() != AiReportPdfStatus.READY || export.getPdfObjectKey() == null) {
            throw new CustomException(ErrorCode.AI_REPORT_PDF_NOT_READY);
        }
        AiReportPdfStorage.DownloadUrl downloadUrl = storage.createDownloadUrl(
                export.getPdfObjectKey(),
                "ai-report-room-" + roomId + "-" + actualType.name().toLowerCase() + ".pdf"
        );
        return new AiReportPdfDownloadUrlRes(downloadUrl.url(), downloadUrl.expiresAt());
    }

    private AiReportPdfType normalize(AiReportPdfType pdfType) {
        return pdfType == null ? AiReportPdfType.BASE : pdfType;
    }

    private boolean hasVisibleCustomReport(AiReport report, Long userId) {
        return report.getCustomReports() != null
                && report.getCustomReports().stream().anyMatch(customReport -> customReport.isVisibleTo(userId));
    }
}
