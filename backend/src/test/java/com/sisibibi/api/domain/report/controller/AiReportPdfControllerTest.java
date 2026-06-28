package com.sisibibi.api.domain.report.controller;

import com.sisibibi.api.domain.report.dto.response.AiReportPdfDownloadUrlRes;
import com.sisibibi.api.domain.report.dto.response.AiReportPdfStatusRes;
import com.sisibibi.api.domain.report.dto.response.AiReportStatusRes;
import com.sisibibi.api.domain.report.service.AiReportPdfCommandService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class AiReportPdfControllerTest {

    private final AiReportPdfCommandService commandService = mock(AiReportPdfCommandService.class);
    private final AiReportPdfController controller = new AiReportPdfController(commandService);

    @Test
    void getStatus_returnsAiReportStatusWithPdf_whenAuthenticated() {
        AuthPrincipal principal = new AuthPrincipal(7L, "user7@example.com", "USER");
        AiReportStatusRes status = new AiReportStatusRes(
                1L, 10L, "COMPLETED", AiReportPdfStatusRes.notStarted(),
                LocalDateTime.of(2026, 6, 28, 12, 0), LocalDateTime.of(2026, 6, 28, 12, 1)
        );
        given(commandService.getStatus(1L, 7L)).willReturn(status);

        ResponseEntity<ApiResponse<AiReportStatusRes>> response = controller.getStatus(1L, principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().reportStatus()).isEqualTo("COMPLETED");
        assertThat(response.getBody().getData().pdf().pdfStatus()).isEqualTo("NOT_STARTED");
    }

    @Test
    void getStatus_throwsUnauthorized_whenPrincipalIsMissing() {
        assertThatThrownBy(() -> controller.getStatus(1L, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void requestPdf_returnsPdfStatus_whenAuthenticated() {
        AuthPrincipal principal = new AuthPrincipal(7L, "user7@example.com", "USER");
        AiReportPdfStatusRes pdfStatus = AiReportPdfStatusRes.notStarted();
        given(commandService.requestPdf(1L, 7L)).willReturn(pdfStatus);

        ResponseEntity<ApiResponse<AiReportPdfStatusRes>> response = controller.requestPdf(1L, principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().pdfStatus()).isEqualTo("NOT_STARTED");
    }

    @Test
    void requestPdf_throwsUnauthorized_whenPrincipalIsMissing() {
        assertThatThrownBy(() -> controller.requestPdf(1L, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void getDownloadUrl_returnsPresignedUrl_whenAuthenticated() {
        AuthPrincipal principal = new AuthPrincipal(7L, "user7@example.com", "USER");
        AiReportPdfDownloadUrlRes urlRes = new AiReportPdfDownloadUrlRes(
                "https://s3.example.com/presigned", Instant.parse("2026-06-28T12:11:10Z")
        );
        given(commandService.createDownloadUrl(1L, 7L)).willReturn(urlRes);

        ResponseEntity<ApiResponse<AiReportPdfDownloadUrlRes>> response = controller.getDownloadUrl(1L, principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().downloadUrl()).isEqualTo("https://s3.example.com/presigned");
    }

    @Test
    void getDownloadUrl_throwsUnauthorized_whenPrincipalIsMissing() {
        assertThatThrownBy(() -> controller.getDownloadUrl(1L, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
