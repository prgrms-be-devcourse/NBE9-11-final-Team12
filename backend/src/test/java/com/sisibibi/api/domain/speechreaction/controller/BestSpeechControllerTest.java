package com.sisibibi.api.domain.speechreaction.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;
import com.sisibibi.api.domain.speechreaction.dto.response.BestSpeechRes;
import com.sisibibi.api.domain.speechreaction.service.SpeechReactionService;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BestSpeechController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        ApiApplication.class,
        BestSpeechController.class,
        GlobalExceptionHandler.class
})
class BestSpeechControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpeechReactionService speechReactionService;

    @Test
    void getBestSpeech_returnsOk() throws Exception {
        given(speechReactionService.getBestSpeech(1L)).willReturn(new BestSpeechRes(
                10L,
                1L,
                20L,
                "베스트 의견",
                SpeechStance.PRO,
                SpeechStatus.READY,
                LocalDateTime.of(2026, 6, 19, 12, 0),
                3L
        ));

        mockMvc.perform(get("/api/v1/rooms/{roomId}/best-speech", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("베스트 의견 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.speechId").value(10))
                .andExpect(jsonPath("$.data.reactionCount").value(3));

        verify(speechReactionService).getBestSpeech(1L);
    }

    @Test
    void getBestSpeech_returnsBadRequest_whenRoomIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{roomId}/best-speech", 0L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }
}
