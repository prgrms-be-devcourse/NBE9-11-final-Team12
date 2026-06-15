package com.sisibibi.api.domain.room.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.room.dto.response.CreateRoomRes;
import com.sisibibi.api.domain.room.dto.response.RoomDetailRes;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.service.RoomService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

@WebMvcTest(AdminRoomController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
    ApiApplication.class,
    AdminRoomController.class,
    GlobalExceptionHandler.class
})
class AdminRoomControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private RoomService roomService;

  @Test
  void createRoom_returnsCreated() throws Exception {
    given(roomService.createRoom(any()))
        .willReturn(new CreateRoomRes(
            10L,
            1L,
            "토론 주제",
            RoomStatus.OPEN,
            LocalDateTime.of(2026, 6, 12, 12, 0),
            LocalDateTime.of(2026, 6, 12, 12, 0)
        ));

    mockMvc.perform(post("/api/v1/admin/rooms")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {
                      "topicId": 1
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value(201))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data.roomId").value(10))
        .andExpect(jsonPath("$.data.topicId").value(1))
        .andExpect(jsonPath("$.data.status").value("OPEN"));

    verify(roomService).createRoom(any());
  }

  @Test
  void createRoom_returnsBadRequest_whenTopicIdIsNull() throws Exception {
    mockMvc.perform(post("/api/v1/admin/rooms")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                    {
                      "topicId": null
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }

  @Test
  void updateRoom_returnsOk() throws Exception {
    given(roomService.updateRoom(any(), any()))
        .willReturn(new RoomDetailRes(
            10L,
            1L,
            "수정 후 제목",
            RoomStatus.OPEN,
            LocalDateTime.of(2026, 6, 15, 10, 0),
            LocalDateTime.of(2026, 6, 15, 12, 0),
            LocalDateTime.of(2026, 6, 14, 10, 0)
        ));

    mockMvc.perform(patch("/api/v1/admin/rooms/{roomId}", 10L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                  {
                    "title": "수정 후 제목",
                    "startedAt": "2026-06-15T10:00:00",
                    "endedAt": "2026-06-15T12:00:00"
                  }
                  """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.message").value("토론방 수정이 완료되었습니다."))
        .andExpect(jsonPath("$.data.roomId").value(10))
        .andExpect(jsonPath("$.data.title").value("수정 후 제목"))
        .andExpect(jsonPath("$.data.status").value("OPEN"));

    verify(roomService).updateRoom(any(), any());
  }

  @Test
  void updateRoom_returnsNotFound_whenRoomDoesNotExist() throws Exception {
    given(roomService.updateRoom(any(), any()))
        .willThrow(new CustomException(ErrorCode.ROOM_NOT_FOUND));

    mockMvc.perform(patch("/api/v1/admin/rooms/{roomId}", 999L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                  {
                    "title": "수정 후 제목"
                  }
                  """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
  }

  @Test
  void updateRoom_returnsBadRequest_whenRoomIdIsNotPositive() throws Exception {
    mockMvc.perform(patch("/api/v1/admin/rooms/{roomId}", 0L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                  {
                    "title": "수정 후 제목"
                  }
                  """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }
}
