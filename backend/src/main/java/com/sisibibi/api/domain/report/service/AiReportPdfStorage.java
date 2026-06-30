package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.entity.AiReportPdfType;

import java.time.Instant;

public interface AiReportPdfStorage {

    String upload(Long roomId, Long reportId, Long userId, AiReportPdfType pdfType, byte[] pdfBytes);

    DownloadUrl createDownloadUrl(String objectKey, String fileName);

    record DownloadUrl(String url, Instant expiresAt) {
    }
}
