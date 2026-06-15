package com.sisibibi.api.domain.speech.report.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.speech.report.dto.response.SpeechReportCreateRes;
import com.sisibibi.api.domain.speech.report.entity.SpeechReportReason;
import com.sisibibi.api.domain.speech.report.entity.SpeechReportStatus;
import com.sisibibi.api.domain.speech.report.service.SpeechReportService;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import com.sisibibi.api.global.security.AuthPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpeechReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        ApiApplication.class,
        SpeechReportController.class,
        GlobalExceptionHandler.class
})
class SpeechReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpeechReportService speechReportService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createReport_returnsCreated() throws Exception {
        given(speechReportService.createReport(any(), any(), any()))
                .willReturn(new SpeechReportCreateRes(
                        100L,
                        10L,
                        SpeechReportReason.HATE_SPEECH,
                        SpeechReportStatus.PENDING,
                        LocalDateTime.of(2026, 6, 14, 12, 0)
                ));

        mockMvc.perform(post("/api/v1/speeches/{speechId}/reports", 10L)
                        .with(authPrincipal(20L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "HATE_SPEECH",
                                  "description": "특정 집단을 비하합니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("의견 신고가 접수되었습니다."))
                .andExpect(jsonPath("$.data.reportId").value(100))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(speechReportService).createReport(any(), any(), any());
    }

    @Test
    void createReport_returnsBadRequest_whenReasonIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/speeches/{speechId}/reports", 10L)
                        .with(authPrincipal(20L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "description": "신고 설명"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data.reason").value("신고 사유는 필수입니다."));
    }

    @Test
    void createReport_returnsBadRequest_whenReasonIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/speeches/{speechId}/reports", 10L)
                        .with(authPrincipal(20L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "INVALID"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void createReport_returnsBadRequest_whenDescriptionExceedsLimit() throws Exception {
        String description = "a".repeat(501);

        mockMvc.perform(post("/api/v1/speeches/{speechId}/reports", 10L)
                        .with(authPrincipal(20L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "OTHER",
                                  "description": "%s"
                                }
                                """.formatted(description)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data.description")
                        .value("신고 상세 설명은 500자 이하여야 합니다."));
    }

    private UsernamePasswordAuthenticationToken authToken(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(userId, "user@example.com", "USER"),
                null,
                List.of()
        );
    }

    private RequestPostProcessor authPrincipal(Long userId) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(authToken(userId));
            return request;
        };
    }
}
