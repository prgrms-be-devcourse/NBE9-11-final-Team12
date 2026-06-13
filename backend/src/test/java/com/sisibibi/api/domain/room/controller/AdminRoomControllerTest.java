package com.sisibibi.api.domain.room.controller;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.room.dto.response.CreateRoomRes;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.service.RoomService;
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
}
