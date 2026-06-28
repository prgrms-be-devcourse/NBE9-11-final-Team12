package com.sisibibi.api.domain.report.dto.request;

import com.sisibibi.api.domain.report.client.dto.AiReportCustomReportPayload;
import com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes;
import com.sisibibi.api.domain.report.service.AiReportGenerationType;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AiReportWorkerCompleteReq(
        @NotNull AiReportGenerationType generationType,
        String coreLine,
        List<String> keyIssues,
        String aiSummary,
        String commonGround,
        String aiOpinion,
        List<AiReportCustomReportPayload> customReports
) {

    public AiReportWorkerCompleteReq {
        keyIssues = keyIssues == null ? List.of() : List.copyOf(keyIssues);
        customReports = customReports == null ? List.of() : List.copyOf(customReports);
    }

    public AiReportGenerateRes toGenerateResponse() {
        return new AiReportGenerateRes(
                coreLine,
                keyIssues,
                aiSummary,
                commonGround,
                aiOpinion,
                customReports
        );
    }
}
