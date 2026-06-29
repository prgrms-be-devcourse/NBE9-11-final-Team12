package com.sisibibi.api.domain.report.service;

import java.time.Instant;

public interface AiReportPdfStorage {

    String upload(Long roomId, Long reportId, Long userId, byte[] pdfBytes);

    DownloadUrl createDownloadUrl(String objectKey, String fileName);

    record DownloadUrl(String url, Instant expiresAt) {
    }
}
