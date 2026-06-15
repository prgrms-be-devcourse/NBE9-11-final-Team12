package com.sisibibi.api.domain.speech.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.speech.dto.response.SpeechCreateRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechCursorPageRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechDetailRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechListRes;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;
import com.sisibibi.api.domain.speech.service.SpeechService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import com.sisibibi.api.global.security.AuthPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

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
                        .with(authPrincipal(2L))
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
                        .with(authPrincipal(2L))
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
                        .with(authPrincipal(2L))
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
    void createMainOpinion_returnsBadRequest_whenContentContainsProfanity() throws Exception {
        given(speechService.createMainOpinion(any(), any(), any()))
                .willThrow(new CustomException(ErrorCode.SPEECH_CONTENT_CONTAINS_PROFANITY));

        mockMvc.perform(post("/api/v1/rooms/{roomId}/speeches", 1L)
                        .with(authPrincipal(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "욕설이 포함된 의견",
                                  "stance": "PRO"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SPEECH_CONTENT_CONTAINS_PROFANITY"))
                .andExpect(jsonPath("$.message")
                        .value("욕설 또는 비속어가 포함된 의견은 등록할 수 없습니다."));
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
                        .with(authPrincipal(2L))
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
                        .with(authPrincipal(2L))
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
    void deleteSpeech_returnsOk() throws Exception {
        doNothing().when(speechService).deleteSpeech(10L, 2L);

        mockMvc.perform(delete("/api/v1/speeches/{speechId}", 10L)
                        .with(authPrincipal(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("내 의견 삭제가 완료되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(speechService).deleteSpeech(10L, 2L);
    }

    @Test
    void deleteSpeech_returnsBadRequest_whenSpeechIdIsNotPositive() throws Exception {
        mockMvc.perform(delete("/api/v1/speeches/{speechId}", 0L)
                        .with(authPrincipal(2L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void updateSpeechLink_returnsOk() throws Exception {
        given(speechService.updateSpeechLink(10L, 2L, "https://example.com/evidence"))
                .willReturn(new SpeechDetailRes(
                        10L,
                        1L,
                        2L,
                        "의견",
                        SpeechStance.PRO,
                        "https://example.com/evidence",
                        null,
                        SpeechStatus.READY,
                        null,
                        null,
                        LocalDateTime.of(2026, 6, 12, 10, 0),
                        LocalDateTime.of(2026, 6, 12, 12, 0)
                ));

        mockMvc.perform(patch("/api/v1/speeches/{speechId}/link", 10L)
                        .with(authPrincipal(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "linkUrl": "https://example.com/evidence"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("근거 링크 첨부가 완료되었습니다."))
                .andExpect(jsonPath("$.data.linkUrl").value("https://example.com/evidence"));

        verify(speechService).updateSpeechLink(10L, 2L, "https://example.com/evidence");
    }

    @Test
    void updateSpeechLink_returnsBadRequest_whenLinkUrlIsBlank() throws Exception {
        mockMvc.perform(patch("/api/v1/speeches/{speechId}/link", 10L)
                        .with(authPrincipal(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "linkUrl": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data.linkUrl").value("근거 링크는 비어 있을 수 없습니다."));
    }

    @Test
    void updateSpeechLink_returnsBadRequest_whenLinkUrlIsInvalid() throws Exception {
        mockMvc.perform(patch("/api/v1/speeches/{speechId}/link", 10L)
                        .with(authPrincipal(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "linkUrl": "example.com/evidence"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.data.linkUrl").value("올바른 링크 형식이어야 합니다."));
    }

    private UsernamePasswordAuthenticationToken authToken(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(userId, "user@example.com", "USER"),
                null,
                List.of()
        );
    }

    private RequestPostProcessor authPrincipal(Long userId) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(authToken(userId));
            return request;
        };
    }
}
