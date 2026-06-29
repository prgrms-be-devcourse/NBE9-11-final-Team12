package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.domain.speech.dto.response.StageCurrentSpeakerRes;
import com.sisibibi.api.domain.speech.dto.response.StageQueueRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestStatusRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.security.AuthPrincipal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentSpeaker_returnsCurrentSpeaker() throws Exception {
        StageCurrentSpeakerRes response = new StageCurrentSpeakerRes(
                true,
                new StageCurrentSpeakerRes.CurrentSpeaker(
                        10L,
                        "logic_hunter",
                        SpeechStance.PRO,
                        3,
                        LocalDateTime.of(2026, 6, 16, 14, 20),
                        LocalDateTime.of(2026, 6, 16, 14, 22)
                )
        );
        given(speakingQueueService.getCurrentSpeaker(1L))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/rooms/1/stage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
                .andExpect(jsonPath("$.data.hasCurrentSpeaker").value(true))
                .andExpect(jsonPath("$.data.currentSpeaker.queueId").doesNotExist())
                .andExpect(jsonPath("$.data.currentSpeaker.userId").value(10))
                .andExpect(jsonPath("$.data.currentSpeaker.nickname")
                        .value("logic_hunter"))
                .andExpect(jsonPath("$.data.currentSpeaker.stance").value("PRO"))
                .andExpect(jsonPath("$.data.currentSpeaker.queueOrder").value(3))
                .andExpect(jsonPath("$.data.currentSpeaker.assignedAt").exists())
                .andExpect(jsonPath("$.data.currentSpeaker.expiresAt").exists())
                .andExpect(jsonPath("$.data.currentSpeaker.isMe").doesNotExist());
    }

    @Test
    void getCurrentSpeaker_returnsEmptyResponseWhenCurrentSpeakerDoesNotExist()
            throws Exception {
        given(speakingQueueService.getCurrentSpeaker(1L))
                .willReturn(StageCurrentSpeakerRes.empty());

        mockMvc.perform(get("/api/v1/rooms/1/stage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasCurrentSpeaker").value(false))
                .andExpect(jsonPath("$.data.currentSpeaker").doesNotExist());
    }

    @Test
    void getCurrentSpeaker_returnsNotFoundWhenRoomDoesNotExist() throws Exception {
        given(speakingQueueService.getCurrentSpeaker(1L))
                .willThrow(new CustomException(ErrorCode.ROOM_NOT_FOUND));

        mockMvc.perform(get("/api/v1/rooms/1/stage"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("존재하지 않는 토론방입니다."));
    }

    @Test
    void getCurrentSpeaker_returnsBadRequestWhenRoomIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/0/stage"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void getQueueSummary_returnsFirstWaitingSpeakers() throws Exception {
        StageQueueRes response = StageQueueRes.of(
                8L,
                0,
                5,
                List.of(
                        new StageQueueRes.WaitingSpeaker(
                                1,
                                10L,
                                "logic_hunter"
                        ),
                        new StageQueueRes.WaitingSpeaker(
                                2,
                                20L,
                                "dream_catcher"
                        )
                )
        );
        given(speakingQueueService.getQueueSummary(1L))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/rooms/1/stage/queue/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalWaitingCount").value(8))
                .andExpect(jsonPath("$.data.offset").value(0))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.items[0].rank").value(1))
                .andExpect(jsonPath("$.data.items[0].userId").value(10))
                .andExpect(jsonPath("$.data.items[0].nickname")
                        .value("logic_hunter"))
                .andExpect(jsonPath("$.data.items[1].rank").value(2))
                .andExpect(jsonPath("$.data.items[1].userId").value(20))
                .andExpect(jsonPath("$.data.items[1].nickname")
                        .value("dream_catcher"));
    }

    @Test
    void getWaitingQueue_returnsPagedWaitingSpeakers() throws Exception {
        StageQueueRes response = StageQueueRes.of(
                4L,
                2,
                2,
                List.of(
                        new StageQueueRes.WaitingSpeaker(
                                3,
                                30L,
                                "neon_wave"
                        ),
                        new StageQueueRes.WaitingSpeaker(
                                4,
                                40L,
                                "open_mind"
                        )
                )
        );
        given(speakingQueueService.getWaitingQueue(1L, 2, 2))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/rooms/1/stage/queue")
                        .param("offset", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalWaitingCount").value(4))
                .andExpect(jsonPath("$.data.offset").value(2))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(false))
                .andExpect(jsonPath("$.data.items[0].rank").value(3))
                .andExpect(jsonPath("$.data.items[0].userId").value(30))
                .andExpect(jsonPath("$.data.items[0].nickname")
                        .value("neon_wave"))
                .andExpect(jsonPath("$.data.items[1].rank").value(4))
                .andExpect(jsonPath("$.data.items[1].userId").value(40))
                .andExpect(jsonPath("$.data.items[1].nickname")
                        .value("open_mind"));
    }

    @Test
    void getWaitingQueue_returnsBadRequestWhenSizeIsNotPositive() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/1/stage/queue")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void requestSpeakingTurn_returnsCreatedResponse() throws Exception {
        StageRequestRes response = new StageRequestRes(
                SpeakingQueueStatus.ASSIGNED,
                1L,
                10L,
                SpeechStance.PRO,
                1,
                LocalDateTime.of(2026, 6, 12, 10, 0)
        );

        given(speakingQueueService.requestSpeakingTurn(1L, 10L, SpeechStance.PRO))
                .willReturn(response);

        mockMvc.perform(post("/api/v1/rooms/1/stage/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stance": "PRO"
                                }
                                """)
                        .with(authPrincipal(10L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("발언권 신청이 완료되었습니다."))
                .andExpect(jsonPath("$.data.roomId").value(1))
                .andExpect(jsonPath("$.data.userId").value(10))
                .andExpect(jsonPath("$.data.stance").value("PRO"))
                .andExpect(jsonPath("$.data.queueOrder").value(1))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.requestedAt").exists())
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.canceledAt").doesNotExist());
    }

    @Test
    void requestSpeakingTurn_returnsCreatedResponseWhenStanceIsMissing() throws Exception {
        StageRequestRes response = new StageRequestRes(
                SpeakingQueueStatus.WAITING,
                1L,
                10L,
                null,
                1,
                LocalDateTime.of(2026, 6, 12, 10, 0)
        );

        given(speakingQueueService.requestSpeakingTurn(1L, 10L, null))
                .willReturn(response);

        mockMvc.perform(post("/api/v1/rooms/1/stage/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(authPrincipal(10L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("발언권 신청이 완료되었습니다."))
                .andExpect(jsonPath("$.data.roomId").value(1))
                .andExpect(jsonPath("$.data.userId").value(10))
                .andExpect(jsonPath("$.data.stance").doesNotExist())
                .andExpect(jsonPath("$.data.queueOrder").value(1))
                .andExpect(jsonPath("$.data.status").value("WAITING"));

        verify(speakingQueueService).requestSpeakingTurn(1L, 10L, null);
    }

    @Test
    void requestSpeakingTurn_returnsConflictWhenDuplicateActiveRequestExists() throws Exception {
        given(speakingQueueService.requestSpeakingTurn(1L, 10L, SpeechStance.PRO))
                .willThrow(new CustomException(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS));

        mockMvc.perform(post("/api/v1/rooms/1/stage/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stance": "PRO"
                                }
                                """)
                        .with(authPrincipal(10L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SPEAKING_REQUEST_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("이미 발언권을 신청한 상태입니다."));
    }

    @Test
    void requestSpeakingTurn_returnsBadRequestWhenRoomIdIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/v1/rooms/0/stage/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stance": "PRO"
                                }
                                """)
                        .with(authPrincipal(10L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void cancelSpeakingRequest_returnsOkResponse() throws Exception {
        mockMvc.perform(delete("/api/v1/rooms/1/stage/requests/me")
                        .with(authPrincipal(10L)))
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
                        .with(authPrincipal(10L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("SPEAKING_REQUEST_NOT_CANCELABLE"))
                .andExpect(jsonPath("$.message")
                        .value("대기 중인 발언권 신청만 취소할 수 있습니다."));
    }

    @Test
    void getMySpeakingRequestStatus_returnsWaitingRequestStatus() throws Exception {
        StageRequestStatusRes response = new StageRequestStatusRes(
                true,
                SpeakingQueueStatus.WAITING,
                1L,
                10L,
                SpeechStance.PRO,
                3,
                2,
                true,
                LocalDateTime.of(2026, 6, 12, 10, 0),
                null,
                null
        );
        given(speakingQueueService.getMySpeakingRequestStatus(1L, 10L))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/rooms/1/stage/requests/me")
                        .with(authPrincipal(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
                .andExpect(jsonPath("$.data.hasRequest").value(true))
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andExpect(jsonPath("$.data.roomId").value(1))
                .andExpect(jsonPath("$.data.userId").value(10))
                .andExpect(jsonPath("$.data.stance").value("PRO"))
                .andExpect(jsonPath("$.data.queueOrder").value(3))
                .andExpect(jsonPath("$.data.currentRank").value(2))
                .andExpect(jsonPath("$.data.cancelable").value(true))
                .andExpect(jsonPath("$.data.requestedAt").exists())
                .andExpect(jsonPath("$.data.assignedAt").doesNotExist())
                .andExpect(jsonPath("$.data.expiresAt").doesNotExist());
    }

    @Test
    void getMySpeakingRequestStatus_returnsEmptyResponseWhenRequestDoesNotExist()
            throws Exception {
        given(speakingQueueService.getMySpeakingRequestStatus(1L, 10L))
                .willReturn(StageRequestStatusRes.empty());

        mockMvc.perform(get("/api/v1/rooms/1/stage/requests/me")
                        .with(authPrincipal(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasRequest").value(false))
                .andExpect(jsonPath("$.data.status").doesNotExist())
                .andExpect(jsonPath("$.data.currentRank").doesNotExist())
                .andExpect(jsonPath("$.data.cancelable").value(false));
    }

    @Test
    void getMySpeakingRequestStatus_returnsNotFoundWhenRoomDoesNotExist()
            throws Exception {
        given(speakingQueueService.getMySpeakingRequestStatus(1L, 10L))
                .willThrow(new CustomException(ErrorCode.ROOM_NOT_FOUND));

        mockMvc.perform(get("/api/v1/rooms/1/stage/requests/me")
                        .with(authPrincipal(10L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("존재하지 않는 토론방입니다."));
    }

    @Test
    void completeSpeakingTurn_returnsOkResponse() throws Exception {
        mockMvc.perform(post("/api/v1/rooms/1/stage/complete")
                        .with(authPrincipal(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("발언이 종료되었습니다."));

        verify(speakingQueueService).completeSpeakingTurn(1L, 10L);
    }

    @Test
    void completeSpeakingTurn_returnsNotFoundWhenCurrentSpeakerDoesNotExist()
            throws Exception {
        willThrow(new CustomException(ErrorCode.CURRENT_SPEAKER_NOT_FOUND))
                .given(speakingQueueService)
                .completeSpeakingTurn(1L, 10L);

        mockMvc.perform(post("/api/v1/rooms/1/stage/complete")
                        .with(authPrincipal(10L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CURRENT_SPEAKER_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("현재 발언자가 존재하지 않습니다."));
    }

    @Test
    void completeSpeakingTurn_returnsForbiddenForDifferentUser() throws Exception {
        willThrow(new CustomException(ErrorCode.FORBIDDEN))
                .given(speakingQueueService)
                .completeSpeakingTurn(1L, 10L);

        mockMvc.perform(post("/api/v1/rooms/1/stage/complete")
                        .with(authPrincipal(10L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private RequestPostProcessor authPrincipal(Long userId) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            new AuthPrincipal(userId, "user@example.com", "USER"),
                            null,
                            List.of()
                    )
            );
            return request;
        };
    }
}
