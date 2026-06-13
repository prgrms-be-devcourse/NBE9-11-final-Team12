package com.sisibibi.api.domain.roomparticipant.cotroller;

import com.sisibibi.api.domain.roomparticipant.dto.response.RoomParticipantRes;
import com.sisibibi.api.domain.roomparticipant.controller.RoomParticipantController;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.service.RoomParticipantService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.security.AuthPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomParticipantController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoomParticipantControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private RoomParticipantService roomParticipantService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void joinRoom_returnsCreated() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new AuthPrincipal(2L, "user@example.com", "USER"),
            null,
            List.of()
        )
    );

    given(roomParticipantService.joinRoom(1L, 2L))
        .willReturn(new RoomParticipantRes(
            10L,
            1L,
            2L,
            RoomParticipantStatus.JOINED,
            LocalDateTime.of(2026, 6, 13, 12, 0)
        ));

    mockMvc.perform(post("/api/v1/rooms/{roomId}/participants", 1L))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value(201))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.message").value("토론방 입장이 완료되었습니다."))
        .andExpect(jsonPath("$.data.roomParticipantId").value(10))
        .andExpect(jsonPath("$.data.roomId").value(1))
        .andExpect(jsonPath("$.data.userId").value(2))
        .andExpect(jsonPath("$.data.status").value("JOINED"));

    verify(roomParticipantService).joinRoom(1L, 2L);
  }

  @Test
  void joinRoom_returnsUnauthorized_whenPrincipalIsMissing() throws Exception {
    mockMvc.perform(post("/api/v1/rooms/{roomId}/participants", 1L))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void joinRoom_returnsBadRequest_whenRoomIdIsNotPositive() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new AuthPrincipal(2L, "user@example.com", "USER"),
            null,
            List.of()
        )
    );

    mockMvc.perform(post("/api/v1/rooms/{roomId}/participants", 0L))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }

  @Test
  void joinRoom_returnsBadRequest_whenRoomIsClosed() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new AuthPrincipal(2L, "user@example.com", "USER"),
            null,
            List.of()
        )
    );

    given(roomParticipantService.joinRoom(1L, 2L))
        .willThrow(new CustomException(ErrorCode.ROOM_CLOSED));

    mockMvc.perform(post("/api/v1/rooms/{roomId}/participants", 1L))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ROOM_CLOSED"));
  }
}