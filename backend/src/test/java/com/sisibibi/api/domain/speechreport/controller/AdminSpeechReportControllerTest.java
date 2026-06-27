package com.sisibibi.api.domain.speechreport.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportDetailRes;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportSummaryRes;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReviewAction;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;
import com.sisibibi.api.domain.speechreport.entity.ViolationSeverity;
import com.sisibibi.api.domain.speechreport.dto.response.SpeechReportReviewRes;
import com.sisibibi.api.domain.speechreport.service.SpeechReportService;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import com.sisibibi.api.global.security.AuthPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminSpeechReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        ApiApplication.class,
        AdminSpeechReportController.class,
        GlobalExceptionHandler.class
})
class AdminSpeechReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpeechReportService speechReportService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getReports_returnsFilteredReportPage() throws Exception {
        given(speechReportService.getReports(
                eq(SpeechReportStatus.PENDING),
                eq(SpeechReportReason.SPAM),
                any()
        )).willReturn(new PageImpl<>(List.of(new SpeechReportSummaryRes(
                100L,
                10L,
                30L,
                "대상자",
                20L,
                "신고자",
                SpeechReportReason.SPAM,
                SpeechReportStatus.PENDING,
                LocalDateTime.of(2026, 6, 21, 12, 0)
        ))));

        mockMvc.perform(get("/api/v1/admin/reports")
                        .queryParam("status", "PENDING")
                        .queryParam("reason", "SPAM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].reportId").value(100))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    @Test
    void getReport_returnsReportDetail() throws Exception {
        given(speechReportService.getReport(100L))
                .willReturn(new SpeechReportDetailRes(
                        100L,
                        10L,
                        30L,
                        "대상자",
                        20L,
                        "신고자",
                        "신고 대상 의견",
                        SpeechReportReason.SPAM,
                        null,
                        SpeechReportStatus.PENDING,
                        null,
                        null,
                        null,
                        null,
                        null,
                        LocalDateTime.of(2026, 6, 21, 12, 0),
                        LocalDateTime.of(2026, 6, 21, 12, 0)
                ));

        mockMvc.perform(get("/api/v1/admin/reports/{reportId}", 100L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value(100))
                .andExpect(jsonPath("$.data.contentSnapshot").value("신고 대상 의견"));
    }

    @Test
    void getReport_returnsBadRequest_whenReportIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/{reportId}", 0L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void reviewReport_returnsReviewResult() throws Exception {
        given(speechReportService.reviewReport(
                eq(100L),
                eq(99L),
                eq(SpeechReportReviewAction.RESOLVE),
                eq("위반 사항 확인"),
                eq(ViolationSeverity.MEDIUM)
        )).willReturn(new SpeechReportReviewRes(
                100L,
                SpeechReportStatus.RESOLVED,
                99L,
                "관리자",
                LocalDateTime.of(2026, 6, 21, 13, 0),
                "위반 사항 확인",
                ViolationSeverity.MEDIUM
        ));

        mockMvc.perform(patch("/api/v1/admin/reports/{reportId}", 100L)
                        .with(authPrincipal(99L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action": "RESOLVE",
                                  "resolutionNote": "위반 사항 확인",
                                  "severity": "MEDIUM"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportId").value(100))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.reviewedBy").value(99))
                .andExpect(jsonPath("$.data.severity").value("MEDIUM"));
    }

    @Test
    void reviewReport_returnsBadRequest_whenActionIsMissing() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/reports/{reportId}", 100L)
                        .with(authPrincipal(99L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resolutionNote": "처리 사유"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.action").value("신고 처리 액션은 필수입니다."));
    }

    private RequestPostProcessor authPrincipal(Long userId) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            new AuthPrincipal(userId, "admin@example.com", "ADMIN"),
                            null,
                            List.of()
                    )
            );
            return request;
        };
    }
}
