package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.speech.dto.response.SpeechCreateRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechCursorPageRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechDetailRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechListRes;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;
import com.sisibibi.api.domain.speech.service.SpeechService;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpeechController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        ApiApplication.class,
        SpeechController.class,
        GlobalExceptionHandler.class
})
class SpeechControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpeechService speechService;

    @Test
    void createMainOpinion_returnsCreated() throws Exception {
        given(speechService.createMainOpinion(any(), any(), any()))
                .willReturn(new SpeechCreateRes(
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

    @Test
    void getSpeeches_returnsOk() throws Exception {
        given(speechService.getSpeeches(1L, null, 20)).willReturn(
                new SpeechCursorPageRes(List.of(new SpeechListRes(
                        10L,
                        1L,
                        2L,
                        "찬성 의견",
                        SpeechStance.PRO,
                        SpeechStatus.READY,
                        LocalDateTime.of(2026, 6, 12, 12, 0)
                )), null, false));

        mockMvc.perform(get("/api/v1/rooms/{roomId}/speeches", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].speechId").value(10))
                .andExpect(jsonPath("$.data.items[0].stance").value("PRO"))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void getSpeeches_returnsBadRequest_whenSizeExceedsLimit() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{roomId}/speeches", 1L)
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void getSpeech_returnsOk() throws Exception {
        given(speechService.getSpeech(10L)).willReturn(new SpeechDetailRes(
                10L,
                1L,
                2L,
                "상세 의견",
                SpeechStance.CON,
                "https://example.com/evidence",
                "https://example.com/image.png",
                SpeechStatus.COMPLETED,
                LocalDateTime.of(2026, 6, 12, 11, 0),
                LocalDateTime.of(2026, 6, 12, 11, 5),
                LocalDateTime.of(2026, 6, 12, 10, 0),
                LocalDateTime.of(2026, 6, 12, 11, 5)
        ));

        mockMvc.perform(get("/api/v1/speeches/{speechId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.speechId").value(10))
                .andExpect(jsonPath("$.data.linkUrl").value("https://example.com/evidence"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void getSpeech_returnsBadRequest_whenSpeechIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/api/v1/speeches/{speechId}", 0L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void updateSpeech_returnsOk() throws Exception {
        given(speechService.updateSpeech(any(), any(), any())).willReturn(new SpeechDetailRes(
                10L,
                1L,
                2L,
                "수정된 의견",
                SpeechStance.PRO,
                null,
                null,
                SpeechStatus.READY,
                null,
                null,
                LocalDateTime.of(2026, 6, 12, 10, 0),
                LocalDateTime.of(2026, 6, 12, 12, 0)
        ));

        mockMvc.perform(patch("/api/v1/speeches/{speechId}", 10L)
                        .header("X-User-Id", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "수정된 의견",
                                  "stance": "PRO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.speechId").value(10))
                .andExpect(jsonPath("$.data.content").value("수정된 의견"))
                .andExpect(jsonPath("$.message").value("내 의견 수정이 완료되었습니다."));

        verify(speechService).updateSpeech(any(), any(), any());
    }

    @Test
    void updateSpeech_returnsBadRequest_whenContentIsBlank() throws Exception {
        mockMvc.perform(patch("/api/v1/speeches/{speechId}", 10L)
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
    void updateSpeech_returnsUnauthorized_whenUserHeaderIsMissing() throws Exception {
        mockMvc.perform(patch("/api/v1/speeches/{speechId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "수정된 의견",
                                  "stance": "PRO"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void deleteSpeech_returnsOk() throws Exception {
        doNothing().when(speechService).deleteSpeech(10L, 2L);

        mockMvc.perform(delete("/api/v1/speeches/{speechId}", 10L)
                        .header("X-User-Id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("내 의견 삭제가 완료되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(speechService).deleteSpeech(10L, 2L);
    }

    @Test
    void deleteSpeech_returnsUnauthorized_whenUserHeaderIsMissing() throws Exception {
        mockMvc.perform(delete("/api/v1/speeches/{speechId}", 10L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void deleteSpeech_returnsBadRequest_whenSpeechIdIsNotPositive() throws Exception {
        mockMvc.perform(delete("/api/v1/speeches/{speechId}", 0L)
                        .header("X-User-Id", 2L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }
}
