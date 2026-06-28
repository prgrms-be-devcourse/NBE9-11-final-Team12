package com.sisibibi.api.domain.room.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.room.dto.response.RoomDetailRes;
import com.sisibibi.api.domain.room.dto.response.RoomSummaryRes;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.service.RoomService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
    ApiApplication.class,
    RoomController.class,
    GlobalExceptionHandler.class
})
class RoomControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private RoomService roomService;

  @Test
  void getOpenRooms_returnsOk() throws Exception {
    given(roomService.getOpenRooms())
        .willReturn(List.of(
            new RoomSummaryRes(
                10L,
                1L,
                "토론방 목록",
                RoomStatus.OPEN,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 11, 0),
                LocalDateTime.of(2026, 6, 15, 9, 50)
            )
        ));

    mockMvc.perform(get("/api/v1/rooms/open"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data[0].roomId").value(10))
        .andExpect(jsonPath("$.data[0].topicId").value(1))
        .andExpect(jsonPath("$.data[0].title").value("진행 중인 토론방"))
        .andExpect(jsonPath("$.data[0].status").value("OPEN"));

    verify(roomService).getOpenRooms();
  }

  @Test
  void getOpenRooms_returnsEmptyList_whenOpenRoomDoesNotExist() throws Exception {
    given(roomService.getOpenRooms()).willReturn(List.of());

    mockMvc.perform(get("/api/v1/rooms/open"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data").isEmpty());

    verify(roomService).getOpenRooms();
  }

  @Test
  void getRooms_returnsOk() throws Exception {
    given(roomService.getRooms())
        .willReturn(List.of(
            new RoomSummaryRes(
                10L,
                1L,
                "토론방 목록",
                RoomStatus.OPEN,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 11, 0),
                LocalDateTime.of(2026, 6, 15, 9, 50)
            )
        ));

    mockMvc.perform(get("/api/v1/rooms"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data[0].roomId").value(10))
        .andExpect(jsonPath("$.data[0].topicId").value(1))
        .andExpect(jsonPath("$.data[0].title").value("토론방 목록"))
        .andExpect(jsonPath("$.data[0].status").value("OPEN"));

    verify(roomService).getRooms();
  }

  @Test
  void getRoom_returnsOk() throws Exception {
    given(roomService.getRoom(10L))
        .willReturn(new RoomDetailRes(
            10L,
            1L,
            "토론방 상세",
            RoomStatus.OPEN,
            LocalDateTime.of(2026, 6, 15, 10, 0),
            null,
            LocalDateTime.of(2026, 6, 15, 10, 0)
        ));

    mockMvc.perform(get("/api/v1/rooms/{roomId}", 10L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data.roomId").value(10))
        .andExpect(jsonPath("$.data.topicId").value(1))
        .andExpect(jsonPath("$.data.title").value("토론방 상세"))
        .andExpect(jsonPath("$.data.status").value("OPEN"));

    verify(roomService).getRoom(10L);
  }

  @Test
  void getRoom_returnsNotFound_whenRoomDoesNotExist() throws Exception {
    given(roomService.getRoom(999L))
        .willThrow(new CustomException(ErrorCode.ROOM_NOT_FOUND));

    mockMvc.perform(get("/api/v1/rooms/{roomId}", 999L))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
  }

  @Test
  void getRoom_returnsBadRequest_whenRoomIdIsNotPositive() throws Exception {
    mockMvc.perform(get("/api/v1/rooms/{roomId}", 0L))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));
  }
}