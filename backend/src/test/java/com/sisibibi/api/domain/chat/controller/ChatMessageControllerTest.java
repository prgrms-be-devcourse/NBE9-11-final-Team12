package com.sisibibi.api.domain.chat.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.chat.dto.response.ChatMessageCursorPageRes;
import com.sisibibi.api.domain.chat.dto.response.ChatMessageRes;
import com.sisibibi.api.domain.chat.service.ChatService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatMessageController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
        ApiApplication.class,
        ChatMessageController.class,
        GlobalExceptionHandler.class
})
class ChatMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMessages_returnsOk() throws Exception {
        given(chatService.getMessages(1L, 2L, null, 50)).willReturn(
                new ChatMessageCursorPageRes(
                        List.of(new ChatMessageRes(
                                10L,
                                1L,
                                2L,
                                "tester",
                                "hello",
                                LocalDateTime.of(2026, 6, 16, 10, 0)
                        )),
                        null,
                        false
                )
        );

        mockMvc.perform(get("/api/v1/rooms/{roomId}/chat/messages", 1L)
                        .with(authPrincipal(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.items[0].messageId").value(10))
                .andExpect(jsonPath("$.data.items[0].nicknameSnapshot").value("tester"))
                .andExpect(jsonPath("$.data.hasNext").value(false));

        verify(chatService).getMessages(1L, 2L, null, 50);
    }

    @Test
    void getMessages_returnsOkWithCursorAndLimit() throws Exception {
        given(chatService.getMessages(1L, 2L, 10L, 20)).willReturn(
                new ChatMessageCursorPageRes(List.of(), null, false)
        );

        mockMvc.perform(get("/api/v1/rooms/{roomId}/chat/messages", 1L)
                        .param("cursor", "10")
                        .param("limit", "20")
                        .with(authPrincipal(2L)))
                .andExpect(status().isOk());

        verify(chatService).getMessages(1L, 2L, 10L, 20);
    }

    @Test
    void getMessages_returnsBadRequest_whenLimitExceedsMax() throws Exception {
        mockMvc.perform(get("/api/v1/rooms/{roomId}/chat/messages", 1L)
                        .param("limit", "51")
                        .with(authPrincipal(2L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
    }

    @Test
    void getMessages_returnsForbidden_whenUserHasNotJoinedRoom() throws Exception {
        given(chatService.getMessages(1L, 2L, null, 50))
                .willThrow(new CustomException(ErrorCode.ROOM_PARTICIPATION_REQUIRED));

        mockMvc.perform(get("/api/v1/rooms/{roomId}/chat/messages", 1L)
                        .with(authPrincipal(2L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROOM_PARTICIPATION_REQUIRED"));
    }

    @Test
    void deleteMessage_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/v1/rooms/{roomId}/chat/messages/{messageId}", 1L, 10L)
                        .with(authPrincipal(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        verify(chatService).deleteMessage(1L, 10L, 2L);
    }

    @Test
    void deleteMessage_returnsForbidden_whenUserIsNotAuthor() throws Exception {
        org.mockito.BDDMockito.willThrow(new CustomException(ErrorCode.FORBIDDEN))
                .given(chatService)
                .deleteMessage(1L, 10L, 2L);

        mockMvc.perform(delete("/api/v1/rooms/{roomId}/chat/messages/{messageId}", 1L, 10L)
                        .with(authPrincipal(2L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
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
