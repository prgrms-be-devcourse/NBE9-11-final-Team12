package com.sisibibi.api.domain.speechreaction.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.speechreaction.dto.response.SpeechReactionCreateRes;
import com.sisibibi.api.domain.speechreaction.service.SpeechReactionService;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import com.sisibibi.api.global.security.AuthPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpeechReactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        ApiApplication.class,
        SpeechReactionController.class,
        GlobalExceptionHandler.class
})
class SpeechReactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpeechReactionService speechReactionService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createReaction_returnsCreated() throws Exception {
        given(speechReactionService.createReaction(10L, 20L))
                .willReturn(new SpeechReactionCreateRes(
                        100L,
                        10L,
                        20L,
                        LocalDateTime.of(2026, 6, 19, 12, 0)
                ));

        mockMvc.perform(post("/api/v1/speeches/{speechId}/reactions", 10L)
                        .with(authPrincipal(20L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("의견 공감이 등록되었습니다."))
                .andExpect(jsonPath("$.data.reactionId").value(100))
                .andExpect(jsonPath("$.data.speechId").value(10));

        verify(speechReactionService).createReaction(10L, 20L);
    }

    @Test
    void createReaction_returnsBadRequest_whenSpeechIdIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/v1/speeches/{speechId}/reactions", 0L)
                        .with(authPrincipal(20L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void deleteReaction_returnsOk() throws Exception {
        doNothing().when(speechReactionService).deleteReaction(10L, 20L);

        mockMvc.perform(delete("/api/v1/speeches/{speechId}/reactions", 10L)
                        .with(authPrincipal(20L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("의견 공감이 취소되었습니다."));

        verify(speechReactionService).deleteReaction(10L, 20L);
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
