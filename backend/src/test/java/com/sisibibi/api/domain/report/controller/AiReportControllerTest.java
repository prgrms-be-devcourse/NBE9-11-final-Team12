package com.sisibibi.api.domain.report.controller;

import com.sisibibi.api.domain.report.dto.request.AiReportGenerateReq;
import com.sisibibi.api.domain.report.dto.response.AiReportPdfStatusRes;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.domain.report.service.AiReportService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AiReportControllerTest {

    private final AiReportService aiReportService = mock(AiReportService.class);
    private final AiReportController controller = new AiReportController(aiReportService);

    @Test
    void getAiReport_returnsUserScopedReport_whenAuthenticated() {
        AuthPrincipal principal = new AuthPrincipal(7L, "user7@example.com", "USER");
        given(aiReportService.getReport(10L, 7L)).willReturn(requestedResponse());

        ResponseEntity<ApiResponse<AiReportRes>> response = controller.getAiReport(10L, principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().status()).isEqualTo("REQUESTED");
        verify(aiReportService).getReport(10L, 7L);
    }

    @Test
    void getAiReport_throwsUnauthorized_whenPrincipalIsMissing() {
        assertThatThrownBy(() -> controller.getAiReport(10L, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void generateAiReport_returnsRequestedReport_whenAuthenticated() {
        AuthPrincipal principal = new AuthPrincipal(7L, "user7@example.com", "USER");
        AiReportGenerateReq request = new AiReportGenerateReq(List.of(
                new AiReportGenerateReq.CustomPromptReq("custom 1", "핵심 쟁점을 더 자세히 정리해줘")
        ));
        given(aiReportService.generateReport(10L, 7L, request)).willReturn(requestedResponse());

        ResponseEntity<ApiResponse<AiReportRes>> response = controller.generateAiReport(10L, principal, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().reportId()).isEqualTo(55L);
        assertThat(response.getBody().getData().status()).isEqualTo("REQUESTED");
        verify(aiReportService).generateReport(10L, 7L, request);
    }

    @Test
    void generateAiReport_usesEmptyRequest_whenBodyIsMissing() {
        AuthPrincipal principal = new AuthPrincipal(7L, "user7@example.com", "USER");
        given(aiReportService.generateReport(10L, 7L, AiReportGenerateReq.empty())).willReturn(requestedResponse());

        ResponseEntity<ApiResponse<AiReportRes>> response = controller.generateAiReport(10L, principal, null);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().status()).isEqualTo("REQUESTED");
        verify(aiReportService).generateReport(10L, 7L, AiReportGenerateReq.empty());
    }

    @Test
    void generateAiReport_throwsUnauthorized_whenPrincipalIsMissing() {
        assertThatThrownBy(() -> controller.generateAiReport(10L, null, AiReportGenerateReq.empty()))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    private AiReportRes requestedResponse() {
        return new AiReportRes(
                55L,
                10L,
                "REQUESTED",
                null,
                List.of(),
                null,
                null,
                null,
                List.of(),
                null,
                LocalDateTime.of(2026, 6, 22, 13, 0),
                null,
                AiReportPdfStatusRes.notStarted()
        );
    }
}
