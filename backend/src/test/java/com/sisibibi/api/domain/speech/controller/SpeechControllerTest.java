package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.speech.dto.SpeechCreateResponse;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;
import com.sisibibi.api.domain.speech.service.SpeechService;
import com.sisibibi.api.global.config.SecurityConfig;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpeechController.class)
@ContextConfiguration(classes = {
        ApiApplication.class,
        SpeechController.class,
        GlobalExceptionHandler.class,
        SecurityConfig.class
})
class SpeechControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpeechService speechService;

    @Test
    void createMainOpinion_returnsCreated() throws Exception {
        given(speechService.createMainOpinion(any(), any(), any()))
                .willReturn(new SpeechCreateResponse(
                        10L,
                        1L,
                        2L,
                        "근거가 있는 찬성 의견입니다.",
                        SpeechStance.PRO,
                        SpeechStatus.READY
                ));

        mockMvc.perform(post("/api/v1/rooms/{roomId}/speeches", 1L)
                        .header("X-User-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "근거가 있는 찬성 의견입니다.",
                                  "stance": "PRO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.speechId").value(10))
                .andExpect(jsonPath("$.data.status").value("READY"));

        verify(speechService).createMainOpinion(any(), any(), any());
    }

    @Test
    void createMainOpinion_returnsBadRequest_whenContentIsBlank() throws Exception {
        mockMvc.perform(post("/api/v1/rooms/{roomId}/speeches", 1L)
                        .header("X-User-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": " ",
                                  "stance": "CON"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data.content").value("의견 내용은 비어 있을 수 없습니다."));
    }

    @Test
    void createMainOpinion_returnsBadRequest_whenStanceIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/rooms/{roomId}/speeches", 1L)
                        .header("X-User-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "의견",
                                  "stance": "NEUTRAL"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void createMainOpinion_returnsUnauthorized_whenUserHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/rooms/{roomId}/speeches", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "의견",
                                  "stance": "PRO"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
