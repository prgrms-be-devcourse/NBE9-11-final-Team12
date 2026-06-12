package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.domain.speech.dto.response.StageCompleteRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.dto.response.CurrentSpeakerRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                1L,
                SpeakingQueueStatus.ASSIGNED,
                1L,
                10L,
                1,
                LocalDateTime.of(2026, 6, 12, 10, 0),
                null,
                LocalDateTime.of(2026, 6, 12, 10, 0),
                null
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
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));
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
    void getCurrentSpeaker_returnsOkResponse() throws Exception {
        CurrentSpeakerRes response = new CurrentSpeakerRes(
                1L,
                SpeakingQueueStatus.ASSIGNED,
                1L,
                10L,
                1,
                LocalDateTime.of(2026, 6, 12, 10, 0),
                LocalDateTime.of(2026, 6, 12, 10, 0)
        );

        given(speakingQueueService.getCurrentSpeaker(1L))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/rooms/1/stage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("현재 발언자 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.roomId").value(1))
                .andExpect(jsonPath("$.data.userId").value(10))
                .andExpect(jsonPath("$.data.queueOrder").value(1))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));
    }

    @Test
    void getCurrentSpeaker_returnsNotFoundWhenCurrentSpeakerDoesNotExist() throws Exception {
        given(speakingQueueService.getCurrentSpeaker(1L))
                .willThrow(new CustomException(ErrorCode.CURRENT_SPEAKER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/rooms/1/stage"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CURRENT_SPEAKER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("현재 발언자가 존재하지 않습니다."));
    }

    @Test
    void completeCurrentSpeaker_returnsOkResponseAndNextSpeaker() throws Exception {
        StageRequestRes completedSpeaker = new StageRequestRes(
                1L,
                SpeakingQueueStatus.COMPLETED,
                1L,
                10L,
                1,
                LocalDateTime.of(2026, 6, 12, 10, 0),
                null,
                null,
                null
        );
        CurrentSpeakerRes nextSpeaker = new CurrentSpeakerRes(
                2L,
                SpeakingQueueStatus.ASSIGNED,
                1L,
                20L,
                2,
                LocalDateTime.of(2026, 6, 12, 10, 1),
                LocalDateTime.of(2026, 6, 12, 10, 5)
        );
        StageCompleteRes response = new StageCompleteRes(1L, completedSpeaker, nextSpeaker);

        given(speakingQueueService.completeCurrentSpeaker(1L, 10L))
                .willReturn(response);

        mockMvc.perform(post("/api/v1/rooms/1/stage/complete")
                        .param("userId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.roomId").value(1))
                .andExpect(jsonPath("$.data.completedSpeaker.userId").value(10))
                .andExpect(jsonPath("$.data.completedSpeaker.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.nextSpeaker.userId").value(20))
                .andExpect(jsonPath("$.data.nextSpeaker.status").value("ASSIGNED"));
    }

    @Test
    void completeCurrentSpeaker_returnsNotFoundWhenCurrentSpeakerDoesNotExist() throws Exception {
        given(speakingQueueService.completeCurrentSpeaker(1L, 10L))
                .willThrow(new CustomException(ErrorCode.CURRENT_SPEAKER_NOT_FOUND));

        mockMvc.perform(post("/api/v1/rooms/1/stage/complete")
                        .param("userId", "10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CURRENT_SPEAKER_NOT_FOUND"));
    }

    @Test
    void completeCurrentSpeaker_returnsForbiddenWhenRequesterIsNotCurrentSpeaker() throws Exception {
        given(speakingQueueService.completeCurrentSpeaker(1L, 10L))
                .willThrow(new CustomException(ErrorCode.FORBIDDEN));

        mockMvc.perform(post("/api/v1/rooms/1/stage/complete")
                        .param("userId", "10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
