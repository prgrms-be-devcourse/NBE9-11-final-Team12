package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.domain.speech.entity.AiCounterIssue;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.service.AiCounterIssuePersistenceService;
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

@WebMvcTest(AiCounterIssueController.class)
@AutoConfigureMockMvc(addFilters = false)
class AiCounterIssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiCounterIssuePersistenceService aiCounterIssuePersistenceService;

    @Test
    void getRecentCounterIssues_returnsCompletedIssues() throws Exception {
        AiCounterIssue issue = AiCounterIssue.pending(1L, 30L, SpeechStance.CON);
        ReflectionTestUtils.setField(issue, "id", 11L);
        ReflectionTestUtils.setField(issue, "createdAt",
                LocalDateTime.of(2026, 6, 25, 14, 0));
        issue.complete(
                "반대 측에서 검토할 핵심 쟁점입니다.",
                LocalDateTime.of(2026, 6, 25, 14, 1)
        );
        given(aiCounterIssuePersistenceService.findRecentCompleted(1L))
                .willReturn(List.of(issue));

        mockMvc.perform(get("/api/v1/rooms/1/ai-counter-issues/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].issueId").value(11))
                .andExpect(jsonPath("$.data[0].roomId").value(1))
                .andExpect(jsonPath("$.data[0].triggerQueueId").value(30))
                .andExpect(jsonPath("$.data[0].targetStance").value("CON"))
                .andExpect(jsonPath("$.data[0].content")
                        .value("반대 측에서 검토할 핵심 쟁점입니다."))
                .andExpect(jsonPath("$.data[0].createdAt").exists())
                .andExpect(jsonPath("$.data[0].completedAt").exists());

        verify(aiCounterIssuePersistenceService).findRecentCompleted(1L);
    }

    @Test
    void getRecentCounterIssues_returnsNotFound_whenRoomDoesNotExist() throws Exception {
        given(aiCounterIssuePersistenceService.findRecentCompleted(999L))
                .willThrow(new CustomException(ErrorCode.ROOM_NOT_FOUND));

        mockMvc.perform(get("/api/v1/rooms/999/ai-counter-issues/recent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    void getRecentCounterIssues_returnsBadRequest_whenRoomIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/0/ai-counter-issues/recent"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }
}
