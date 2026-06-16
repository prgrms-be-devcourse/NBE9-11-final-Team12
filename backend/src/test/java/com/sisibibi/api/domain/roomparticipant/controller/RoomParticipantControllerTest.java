package com.sisibibi.api.domain.roomparticipant.controller;

import com.sisibibi.api.domain.roomparticipant.dto.response.RoomParticipantRes;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

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

  @Test
  void leaveRoom_returnsOk() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new AuthPrincipal(2L, "user@example.com", "USER"),
            null,
            List.of()
        )
    );

    mockMvc.perform(post("/api/v1/rooms/{roomId}/participants/out", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.message").value("토론방 퇴장이 완료되었습니다."));

    verify(roomParticipantService).leaveRoom(1L, 2L);
  }

  @Test
  void getRoomParticipants_returnsOk() throws Exception {
    given(roomParticipantService.getRoomParticipants(1L))
        .willReturn(List.of(
            new RoomParticipantRes(
                10L,
                1L,
                2L,
                RoomParticipantStatus.JOINED,
                LocalDateTime.of(2026, 6, 15, 12, 0)
            ),
            new RoomParticipantRes(
                11L,
                1L,
                3L,
                RoomParticipantStatus.JOINED,
                LocalDateTime.of(2026, 6, 15, 12, 1)
            )
        ));

    mockMvc.perform(get("/api/v1/rooms/{roomId}/participants", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data[0].roomParticipantId").value(10))
        .andExpect(jsonPath("$.data[0].roomId").value(1))
        .andExpect(jsonPath("$.data[0].userId").value(2))
        .andExpect(jsonPath("$.data[0].status").value("JOINED"))
        .andExpect(jsonPath("$.data[1].roomParticipantId").value(11))
        .andExpect(jsonPath("$.data[1].userId").value(3));

    verify(roomParticipantService).getRoomParticipants(1L);
  }

  @Test
  void getRoomParticipants_returnsEmptyList_whenJoinedParticipantDoesNotExist() throws Exception {
    given(roomParticipantService.getRoomParticipants(1L)).willReturn(List.of());

    mockMvc.perform(get("/api/v1/rooms/{roomId}/participants", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data").isEmpty());

    verify(roomParticipantService).getRoomParticipants(1L);
  }

  @Test
  void getRoomParticipants_returnsNotFound_whenRoomDoesNotExist() throws Exception {
    given(roomParticipantService.getRoomParticipants(999L))
        .willThrow(new CustomException(ErrorCode.ROOM_NOT_FOUND));

    mockMvc.perform(get("/api/v1/rooms/{roomId}/participants", 999L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
  }

  @Test
  void getRoomParticipants_returnsBadRequest_whenRoomIdIsNotPositive() throws Exception {
    mockMvc.perform(get("/api/v1/rooms/{roomId}/participants", 0L))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }
}