package com.sisibibi.api.domain.payment.service;

import com.sisibibi.api.domain.payment.entity.CustomAiReportRequest;
import com.sisibibi.api.domain.payment.repository.CustomAiReportRequestRepository;
import com.sisibibi.api.domain.report.entity.AiReportPdfType;
import com.sisibibi.api.domain.report.prompt.CustomPromptCommand;
import com.sisibibi.api.domain.report.queue.AiReportQueueMessage;
import com.sisibibi.api.domain.report.queue.AiReportQueuePublisher;
import com.sisibibi.api.domain.report.service.AiReportGenerationType;
import com.sisibibi.api.domain.report.service.AiReportPdfPersistenceService;
import com.sisibibi.api.domain.report.service.AiReportPersistenceService;
import com.sisibibi.api.domain.report.service.AiReportRequestResult;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomAiReportGenerationDispatchService {

  private final CustomAiReportRequestRepository customAiReportRequestRepository;
  private final AiReportPersistenceService aiReportPersistenceService;
  private final AiReportQueuePublisher aiReportQueuePublisher;
  private final AiReportPdfPersistenceService aiReportPdfPersistenceService;

  public void dispatch(Long customAiReportRequestId) {
    CustomAiReportRequest request = customAiReportRequestRepository.findById(customAiReportRequestId)
        .orElseThrow(() -> new IllegalStateException("Custom AI report request not found."));

    AiReportRequestResult result = aiReportPersistenceService.requestCustomGeneration(
        request.getRoomId(),
        request.getUserId(),
        request.getCustomPrompts().stream()
            .map(prompt -> new CustomPromptCommand(
                prompt.label(),
                prompt.prompt()
            ))
            .toList()
    );

    Long reportId = result.response().reportId();
    aiReportPdfPersistenceService.createIfMissing(reportId, request.getRoomId(), request.getUserId(), AiReportPdfType.CUSTOM);

    try {
      aiReportQueuePublisher.publish(new AiReportQueueMessage(
          reportId,
          request.getRoomId(),
          AiReportGenerationType.CUSTOM_ONLY,
          "custom-ai-report-request-" + request.getId() + "-v1"
      ));
      if (hasBaseReportContent(result)) {
        aiReportPersistenceService.markQueued(reportId);
      }
      request.markQueued();
      customAiReportRequestRepository.save(request);
    } catch (RuntimeException publishException) {
      if (hasBaseReportContent(result)) {
        aiReportPersistenceService.markPublishFailed(
            reportId,
            ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.name(),
            ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.getMessage()
        );
      }
      request.markPublishFailed(ErrorCode.AI_REPORT_QUEUE_PUBLISH_FAILED.getMessage());
      customAiReportRequestRepository.save(request);
      throw publishException;
    }
  }

  private boolean hasBaseReportContent(AiReportRequestResult result) {
    return result.response().coreLine() != null
        && result.response().keyIssues() != null
        && result.response().aiSummary() != null
        && result.response().commonGround() != null
        && result.response().aiOpinion() != null;
  }
}
