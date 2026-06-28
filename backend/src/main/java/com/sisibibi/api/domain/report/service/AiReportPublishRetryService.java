package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.entity.AiReport;
import com.sisibibi.api.domain.report.entity.AiReportStatus;
import com.sisibibi.api.domain.report.queue.AiReportQueueMessage;
import com.sisibibi.api.domain.report.queue.AiReportQueueProperties;
import com.sisibibi.api.domain.report.queue.AiReportQueuePublisher;
import com.sisibibi.api.domain.report.repository.AiReportRepository;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiReportPublishRetryService {

    private static final List<AiReportStatus> RETRYABLE_STATUSES = List.of(
            AiReportStatus.REQUESTED,
            AiReportStatus.PUBLISH_FAILED
    );

    private final AiReportRepository aiReportRepository;
    private final AiReportQueuePublisher aiReportQueuePublisher;
    private final AiReportPersistenceService aiReportPersistenceService;
    private final AiReportQueueProperties aiReportQueueProperties;

    public int republishStaleRequests() {
        return republishStaleRequests(LocalDateTime.now());
    }

    int republishStaleRequests(LocalDateTime now) {
        AiReportQueueProperties.Retry retry = aiReportQueueProperties.getRetry();
        LocalDateTime updatedBefore = now.minus(retry.getStaleThreshold());
        int batchSize = Math.max(1, retry.getBatchSize());

        List<AiReport> candidates = aiReportRepository.findPublishRetryCandidates(
                RETRYABLE_STATUSES,
                updatedBefore,
                retry.getMaxRetryCount(),
                PageRequest.of(0, batchSize)
        );

        int successCount = 0;
        for (AiReport report : candidates) {
            if (republish(report)) {
                successCount++;
            }
        }

        return successCount;
    }

    private boolean republish(AiReport report) {
        try {
            aiReportQueuePublisher.publish(AiReportQueueMessage.of(
                    report.getId(),
                    report.getRoomId(),
                    resolveGenerationType(report)
            ));
            aiReportPersistenceService.markQueued(report.getId());
            return true;
        } catch (RuntimeException publishException) {
            log.warn(
                    "Failed to republish AI report generation message. reportId={}, roomId={}",
                    report.getId(),
                    report.getRoomId(),
                    publishException
            );
            aiReportPersistenceService.markPublishFailed(
                    report.getId(),
                    ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.name(),
                    ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.getMessage()
            );
            return false;
        }
    }

    private AiReportGenerationType resolveGenerationType(AiReport report) {
        if (report.getCustomPrompts() == null || report.getCustomPrompts().isEmpty()) {
            return AiReportGenerationType.BASE_ONLY;
        }

        if (hasBaseReport(report)) {
            return AiReportGenerationType.CUSTOM_ONLY;
        }

        return AiReportGenerationType.BASE_WITH_CUSTOM;
    }

    private boolean hasBaseReport(AiReport report) {
        return hasText(report.getCoreLine())
                || (report.getKeyIssues() != null && !report.getKeyIssues().isEmpty())
                || hasText(report.getAiSummary())
                || hasText(report.getCommonGround())
                || hasText(report.getAiOpinion());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
