package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.client.AiReportClient;
import com.sisibibi.api.domain.report.client.dto.AiReportGenerateReq;
import com.sisibibi.api.domain.report.client.dto.AiReportGenerateRes;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiReportServiceTest {

    @Mock
    private AiReportPersistenceService aiReportPersistenceService;

    @Mock
    private AiReportClient aiReportClient;

    @InjectMocks
    private AiReportService aiReportService;

    @Test
    void generateReport_callsAiServerAndCompletesReport_whenPreparationRequiresAiCall() {
        AiReportGenerateReq request = new AiReportGenerateReq(null, null, List.of());
        AiReportGenerateRes aiResponse = new AiReportGenerateRes(
                "핵심 한줄",
                List.of("쟁점 1", "쟁점 2"),
                "종합 정리",
                "공통 의견",
                "개인적 소견"
        );
        AiReportRes completed = new AiReportRes(
                55L,
                10L,
                "COMPLETED",
                "핵심 한줄",
                List.of("쟁점 1", "쟁점 2"),
                "종합 정리",
                "공통 의견",
                "개인적 소견",
                null,
                null,
                null
        );

        given(aiReportPersistenceService.prepareGeneration(10L))
                .willReturn(AiReportGenerationContext.callAi(55L, request));
        given(aiReportClient.generate(request)).willReturn(aiResponse);
        given(aiReportPersistenceService.complete(55L, aiResponse)).willReturn(completed);

        AiReportRes result = aiReportService.generateReport(10L);

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.coreLine()).isEqualTo("핵심 한줄");
        verify(aiReportClient).generate(request);
        verify(aiReportPersistenceService).complete(55L, aiResponse);
    }

    @Test
    void generateReport_returnsExistingReportWithoutCallingAiServer_whenReportAlreadyExists() {
        AiReportRes pending = new AiReportRes(
                55L,
                10L,
                "PENDING",
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null
        );

        given(aiReportPersistenceService.prepareGeneration(10L))
                .willReturn(AiReportGenerationContext.skipAi(pending));

        AiReportRes result = aiReportService.generateReport(10L);

        assertThat(result.status()).isEqualTo("PENDING");
        verify(aiReportClient, never()).generate(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void generateReport_marksFailed_whenAiServerCallFails() {
        AiReportGenerateReq request = new AiReportGenerateReq(null, null, List.of());
        AiReportRes failed = new AiReportRes(
                55L,
                10L,
                "FAILED",
                null,
                List.of(),
                null,
                null,
                null,
                "AI 리포트 생성에 실패했습니다.",
                null,
                null
        );

        given(aiReportPersistenceService.prepareGeneration(10L))
                .willReturn(AiReportGenerationContext.callAi(55L, request));
        given(aiReportClient.generate(request))
                .willThrow(new CustomException(ErrorCode.AI_REPORT_GENERATE_FAILED));
        given(aiReportPersistenceService.fail(55L, ErrorCode.AI_REPORT_GENERATE_FAILED.getMessage()))
                .willReturn(failed);

        AiReportRes result = aiReportService.generateReport(10L);

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.errorMessage()).isEqualTo("AI 리포트 생성에 실패했습니다.");
        verify(aiReportPersistenceService).fail(55L, ErrorCode.AI_REPORT_GENERATE_FAILED.getMessage());
    }

    @Test
    void generateReport_marksFailed_whenAiServerResponseMissesRequiredField() {
        AiReportGenerateReq request = new AiReportGenerateReq(null, null, List.of());
        AiReportGenerateRes invalidResponse = new AiReportGenerateRes(
                "",
                List.of("쟁점"),
                "종합 정리",
                "공통 의견",
                "개인적 소견"
        );

        given(aiReportPersistenceService.prepareGeneration(10L))
                .willReturn(AiReportGenerationContext.callAi(55L, request));
        given(aiReportClient.generate(request)).willReturn(invalidResponse);

        aiReportService.generateReport(10L);

        verify(aiReportPersistenceService).fail(55L, ErrorCode.AI_REPORT_INVALID_RESPONSE.getMessage());
        verify(aiReportPersistenceService, never()).complete(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }
}
