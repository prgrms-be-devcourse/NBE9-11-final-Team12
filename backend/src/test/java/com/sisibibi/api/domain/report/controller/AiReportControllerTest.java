package com.sisibibi.api.domain.report.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.report.dto.response.AiReportRes;
import com.sisibibi.api.domain.report.service.AiReportService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        ApiApplication.class,
        AiReportController.class,
        GlobalExceptionHandler.class
})
class AiReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiReportService aiReportService;

    @Test
    void getAiReport_returnsPendingStatus() throws Exception {
        given(aiReportService.getReport(10L)).willReturn(new AiReportRes(
                55L,
                10L,
                "PENDING",
                null,
                List.of(),
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 6, 22, 13, 0),
                null
        ));

        mockMvc.perform(get("/api/v1/rooms/{roomId}/ai-report", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.reportId").value(55))
                .andExpect(jsonPath("$.data.roomId").value(10))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(aiReportService).getReport(10L);
    }

    @Test
    void getAiReport_returnsNotFound_whenReportDoesNotExist() throws Exception {
        given(aiReportService.getReport(10L))
                .willThrow(new CustomException(ErrorCode.AI_REPORT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/rooms/{roomId}/ai-report", 10L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AI_REPORT_NOT_FOUND"));
    }

    @Test
    void generateAiReport_returnsCompletedReport() throws Exception {
        given(aiReportService.generateReport(10L)).willReturn(new AiReportRes(
                55L,
                10L,
                "COMPLETED",
                "핵심 한줄",
                List.of("쟁점 1"),
                "종합 정리",
                "공통 의견",
                "개인적 소견",
                null,
                LocalDateTime.of(2026, 6, 22, 13, 0),
                LocalDateTime.of(2026, 6, 22, 13, 1)
        ));

        mockMvc.perform(post("/api/v1/rooms/{roomId}/ai-report", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.coreLine").value("핵심 한줄"))
                .andExpect(jsonPath("$.data.keyIssues[0]").value("쟁점 1"));

        verify(aiReportService).generateReport(10L);
    }
}
