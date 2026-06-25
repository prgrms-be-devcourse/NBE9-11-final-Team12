package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.domain.speech.entity.StageSummary;
import com.sisibibi.api.domain.speech.service.StageSummaryPersistenceService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StageSummaryController.class)
@AutoConfigureMockMvc(addFilters = false)
class StageSummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StageSummaryPersistenceService stageSummaryPersistenceService;

    @Test
    void getStageSummary_returnsCompletedSummary() throws Exception {
        StageSummary summary = StageSummary.pending(
                1L,
                LocalDateTime.of(2026, 6, 26, 11, 0),
                10
        );
        ReflectionTestUtils.setField(summary, "id", 77L);
        ReflectionTestUtils.setField(summary, "createdAt",
                LocalDateTime.of(2026, 6, 26, 11, 0));
        ReflectionTestUtils.setField(summary, "updatedAt",
                LocalDateTime.of(2026, 6, 26, 11, 1));
        summary.complete(
                "지금까지는 양측의 핵심 쟁점이 정리되고 있습니다.",
                List.of("접근성", "안전성", "책임 소재"),
                12,
                LocalDateTime.of(2026, 6, 26, 11, 1)
        );
        given(stageSummaryPersistenceService.getSummary(1L)).willReturn(summary);

        mockMvc.perform(get("/api/v1/rooms/1/stage-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.summaryId").value(77))
                .andExpect(jsonPath("$.data.roomId").value(1))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.moderatorSummary")
                        .value("지금까지는 양측의 핵심 쟁점이 정리되고 있습니다."))
                .andExpect(jsonPath("$.data.keyPoints[0]").value("접근성"))
                .andExpect(jsonPath("$.data.keyPoints[1]").value("안전성"))
                .andExpect(jsonPath("$.data.keyPoints[2]").value("책임 소재"))
                .andExpect(jsonPath("$.data.speechCount").value(12))
                .andExpect(jsonPath("$.data.completedSpeakerCount").value(10))
                .andExpect(jsonPath("$.data.triggeredAt").exists())
                .andExpect(jsonPath("$.data.completedAt").exists())
                .andExpect(jsonPath("$.data.errorMessage").doesNotExist());

        verify(stageSummaryPersistenceService).getSummary(1L);
    }

    @Test
    void getStageSummary_returnsNotFound_whenSummaryDoesNotExist() throws Exception {
        given(stageSummaryPersistenceService.getSummary(1L))
                .willThrow(new CustomException(ErrorCode.STAGE_SUMMARY_NOT_FOUND));

        mockMvc.perform(get("/api/v1/rooms/1/stage-summary"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STAGE_SUMMARY_NOT_FOUND"));
    }
}
