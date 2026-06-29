package com.sisibibi.api.domain.report.controller;

import com.sisibibi.api.domain.report.client.dto.AiReportGenerateReq;
import com.sisibibi.api.domain.report.dto.request.AiReportWorkerCompleteReq;
import com.sisibibi.api.domain.report.dto.request.AiReportWorkerFailReq;
import com.sisibibi.api.domain.report.dto.request.AiReportWorkerProcessingReq;
import com.sisibibi.api.domain.report.dto.response.AiReportPdfStatusRes;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.domain.report.dto.response.AiReportWorkerProcessingRes;
import com.sisibibi.api.domain.report.service.AiReportGenerationType;
import com.sisibibi.api.domain.report.service.AiReportWorkerAuthService;
import com.sisibibi.api.domain.report.service.AiReportWorkerCallbackService;
import com.sisibibi.api.global.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiReportWorkerCallbackControllerTest {

    private final AiReportWorkerCallbackService callbackService = mock(AiReportWorkerCallbackService.class);
    private final AiReportWorkerAuthService authService = mock(AiReportWorkerAuthService.class);
    private final AiReportWorkerCallbackController controller =
            new AiReportWorkerCallbackController(callbackService, authService);

    @Test
    void startProcessing_validatesTokenAndReturnsGenerationInput() {
        AiReportWorkerProcessingReq request =
                new AiReportWorkerProcessingReq(AiReportGenerationType.BASE_ONLY);
        AiReportWorkerProcessingRes processingResponse = new AiReportWorkerProcessingRes(
                55L,
                10L,
                AiReportGenerationType.BASE_ONLY,
                new AiReportGenerateReq(null, null, List.of())
        );
        given(callbackService.startProcessing(55L, request.generationType()))
                .willReturn(processingResponse);

        ResponseEntity<ApiResponse<AiReportWorkerProcessingRes>> response =
                controller.startProcessing(55L, "worker-token", request);

        verify(authService).validate("worker-token");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(processingResponse);
    }

    @Test
    void complete_validatesTokenAndDelegatesToService() {
        AiReportWorkerCompleteReq request = completeRequest();
        AiReportRes completed = completedResponse();
        given(callbackService.complete(55L, request)).willReturn(completed);

        ResponseEntity<ApiResponse<AiReportRes>> response =
                controller.complete(55L, "worker-token", request);

        verify(authService).validate("worker-token");
        verify(callbackService).complete(55L, request);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().status()).isEqualTo("COMPLETED");
    }

    @Test
    void fail_validatesTokenAndDelegatesToService() {
        AiReportWorkerFailReq request =
                new AiReportWorkerFailReq("LLM_TIMEOUT", "local LLM timed out");
        AiReportRes failed = failedResponse();
        given(callbackService.fail(55L, request)).willReturn(failed);

        ResponseEntity<ApiResponse<AiReportRes>> response =
                controller.fail(55L, "worker-token", request);

        verify(authService).validate("worker-token");
        verify(callbackService).fail(55L, request);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().status()).isEqualTo("GENERATION_FAILED");
    }

    private AiReportWorkerCompleteReq completeRequest() {
        return new AiReportWorkerCompleteReq(
                AiReportGenerationType.BASE_ONLY,
                "core",
                List.of("issue"),
                "summary",
                "common",
                "opinion",
                List.of()
        );
    }

    private AiReportRes completedResponse() {
        return new AiReportRes(
                55L,
                10L,
                "COMPLETED",
                "core",
                List.of("issue"),
                "summary",
                "common",
                "opinion",
                List.of(),
                null,
                LocalDateTime.of(2026, 6, 25, 12, 0),
                LocalDateTime.of(2026, 6, 25, 12, 2),
                AiReportPdfStatusRes.notStarted()
        );
    }

    private AiReportRes failedResponse() {
        return new AiReportRes(
                55L,
                10L,
                "GENERATION_FAILED",
                null,
                List.of(),
                null,
                null,
                null,
                List.of(),
                "local LLM timed out",
                LocalDateTime.of(2026, 6, 25, 12, 0),
                null,
                AiReportPdfStatusRes.notStarted()
        );
    }
}
