package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StageController.class)
@AutoConfigureMockMvc(addFilters = false)
class StageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpeakingQueueService speakingQueueService;

    @Test
    void requestSpeakingTurn_returnsCreatedResponse() throws Exception {
        StageRequestRes response = new StageRequestRes(
                SpeakingQueueStatus.ASSIGNED,
                1L,
                10L,
                1,
                LocalDateTime.of(2026, 6, 12, 10, 0)
        );

        given(speakingQueueService.requestSpeakingTurn(1L, 10L))
                .willReturn(response);

        mockMvc.perform(post("/api/v1/rooms/1/stage/requests")
                        .param("userId", "10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("발언권 신청이 완료되었습니다."))
                .andExpect(jsonPath("$.data.roomId").value(1))
                .andExpect(jsonPath("$.data.userId").value(10))
                .andExpect(jsonPath("$.data.queueOrder").value(1))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.requestedAt").exists())
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.canceledAt").doesNotExist());
    }

    @Test
    void requestSpeakingTurn_returnsConflictWhenDuplicateActiveRequestExists() throws Exception {
        given(speakingQueueService.requestSpeakingTurn(1L, 10L))
                .willThrow(new CustomException(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS));

        mockMvc.perform(post("/api/v1/rooms/1/stage/requests")
                        .param("userId", "10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SPEAKING_REQUEST_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("이미 발언권을 신청한 상태입니다."));
    }

    @Test
    void requestSpeakingTurn_returnsBadRequestWhenUserIdIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/rooms/1/stage/requests"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void requestSpeakingTurn_returnsBadRequestWhenUserIdIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/v1/rooms/1/stage/requests")
                        .param("userId", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void cancelSpeakingRequest_returnsOkResponse() throws Exception {
        mockMvc.perform(delete("/api/v1/rooms/1/stage/requests/me")
                        .param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("발언권 신청이 취소되었습니다."));

        verify(speakingQueueService).cancelSpeakingRequest(1L, 10L);
    }

    @Test
    void cancelSpeakingRequest_returnsConflictWhenRequestIsAssigned() throws Exception {
        willThrow(new CustomException(ErrorCode.SPEAKING_REQUEST_NOT_CANCELABLE))
                .given(speakingQueueService)
                .cancelSpeakingRequest(1L, 10L);

        mockMvc.perform(delete("/api/v1/rooms/1/stage/requests/me")
                        .param("userId", "10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("SPEAKING_REQUEST_NOT_CANCELABLE"))
                .andExpect(jsonPath("$.message")
                        .value("대기 중인 발언권 신청만 취소할 수 있습니다."));
    }

    @Test
    void cancelSpeakingRequest_returnsBadRequestWhenUserIdIsMissing() throws Exception {
        mockMvc.perform(delete("/api/v1/rooms/1/stage/requests/me"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }
}
