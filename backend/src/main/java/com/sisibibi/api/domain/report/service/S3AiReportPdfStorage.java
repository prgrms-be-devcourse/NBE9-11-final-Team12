package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.storage.S3StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3AiReportPdfStorage implements AiReportPdfStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3StorageProperties storageProperties;
    private final AiReportPdfProperties pdfProperties;

    @Override
    public String upload(Long roomId, Long reportId, Long userId, byte[] pdfBytes) {
        validateConfig();
        String objectKey = "ai-reports/%d/%d/%d.pdf".formatted(roomId, reportId, userId);
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(storageProperties.getBucket())
                            .key(objectKey)
                            .contentType("application/pdf")
                            .contentLength((long) pdfBytes.length)
                            .build(),
                    RequestBody.fromBytes(pdfBytes)
            );
            return objectKey;
        } catch (RuntimeException e) {
            log.warn("Failed to upload AI report PDF. objectKey={}", objectKey, e);
            throw new CustomException(ErrorCode.AI_REPORT_PDF_GENERATE_FAILED);
        }
    }

    @Override
    public DownloadUrl createDownloadUrl(String objectKey, String fileName) {
        validateConfig();
        try {
            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(storageProperties.getBucket())
                    .key(objectKey)
                    .responseContentType("application/pdf")
                    .responseContentDisposition("attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedName)
                    .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(pdfProperties.getPresignedUrlExpiration())
                    .getObjectRequest(request)
                    .build();
            Instant expiresAt = Instant.now().plus(pdfProperties.getPresignedUrlExpiration());
            return new DownloadUrl(s3Presigner.presignGetObject(presignRequest).url().toString(), expiresAt);
        } catch (RuntimeException e) {
            log.warn("Failed to create AI report PDF download URL. objectKey={}", objectKey, e);
            throw new CustomException(ErrorCode.AI_REPORT_PDF_DOWNLOAD_FAILED);
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(storageProperties.getBucket()) || !StringUtils.hasText(storageProperties.getRegion())) {
            throw new CustomException(ErrorCode.S3_CONFIG_MISSING);
        }
    }
}
