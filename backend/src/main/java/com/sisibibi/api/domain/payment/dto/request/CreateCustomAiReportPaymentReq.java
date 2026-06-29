package com.sisibibi.api.domain.payment.dto.request;

import com.sisibibi.api.domain.report.dto.request.AiReportGenerateReq;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateCustomAiReportPaymentReq(
    @NotNull @Positive Long roomId,
    @Positive long amount,
    List<AiReportGenerateReq.CustomPromptReq> customPrompts
) {
  public AiReportGenerateReq toAiReportGenerateReq() {
    return new AiReportGenerateReq(customPrompts);
  }
}