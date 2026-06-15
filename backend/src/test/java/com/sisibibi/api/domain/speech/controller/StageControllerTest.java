package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

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
                        .with(authPrincipal(10L)))
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
                        .with(authPrincipal(10L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SPEAKING_REQUEST_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("이미 발언권을 신청한 상태입니다."));
    }

    @Test
    void requestSpeakingTurn_returnsUnauthorizedWhenPrincipalIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/rooms/1/stage/requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void requestSpeakingTurn_returnsBadRequestWhenRoomIdIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/v1/rooms/0/stage/requests")
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
    void cancelSpeakingRequest_returnsUnauthorizedWhenPrincipalIsMissing() throws Exception {
        mockMvc.perform(delete("/api/v1/rooms/1/stage/requests/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
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
