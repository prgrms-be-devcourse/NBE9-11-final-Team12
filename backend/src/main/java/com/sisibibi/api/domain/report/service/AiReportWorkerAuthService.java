package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.client.AiReportProperties;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
public class AiReportWorkerAuthService {

    private final AiReportProperties aiReportProperties;

    public void validate(String token) {
        String expectedToken = aiReportProperties.getWorkerToken();
        if (!StringUtils.hasText(expectedToken)
                || !StringUtils.hasText(token)
                || !matches(expectedToken, token)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }

    private boolean matches(String expectedToken, String actualToken) {
        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                actualToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
