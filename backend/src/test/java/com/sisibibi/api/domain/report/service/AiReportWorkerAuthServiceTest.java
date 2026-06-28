package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.client.AiReportProperties;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiReportWorkerAuthServiceTest {

    @Test
    void validate_acceptsMatchingWorkerToken() {
        AiReportProperties properties = new AiReportProperties();
        properties.setWorkerToken("secret");
        AiReportWorkerAuthService authService = new AiReportWorkerAuthService(properties);

        authService.validate("secret");
    }

    @Test
    void validate_rejectsMismatchedWorkerToken() {
        AiReportProperties properties = new AiReportProperties();
        properties.setWorkerToken("secret");
        AiReportWorkerAuthService authService = new AiReportWorkerAuthService(properties);

        assertThatThrownBy(() -> authService.validate("wrong"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    void validate_rejectsWhenWorkerTokenIsNotConfigured() {
        AiReportProperties properties = new AiReportProperties();
        AiReportWorkerAuthService authService = new AiReportWorkerAuthService(properties);

        assertThatThrownBy(() -> authService.validate("secret"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
