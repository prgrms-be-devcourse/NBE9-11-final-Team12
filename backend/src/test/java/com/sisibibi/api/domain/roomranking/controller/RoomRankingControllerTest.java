package com.sisibibi.api.domain.roomranking.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sisibibi.api.ApiApplication;
import com.sisibibi.api.domain.room.dto.response.RoomSummaryRes;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.service.RoomService;
import com.sisibibi.api.domain.roomranking.dto.response.RoomRankingRes;
import com.sisibibi.api.domain.roomranking.service.RoomRankingService;
import com.sisibibi.api.global.exception.GlobalExceptionHandler;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RoomRankingController.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = {
    ApiApplication.class,
    RoomRankingController.class,
    GlobalExceptionHandler.class
})
class RoomRankingControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private RoomService roomService;

  @MockitoBean
  private RoomRankingService roomRankingService;

  @Test
  void getRoomRanking_returnsOk() throws Exception {
    given(roomRankingService.getRankedOpenRooms())
        .willReturn(List.of(new RoomRankingRes(
            null,
            1.23,
            new RoomSummaryRes(
                10L,
                1L,
                "인기 토론방",
                RoomStatus.OPEN,
                LocalDateTime.of(2026, 6, 15, 10, 0),
                LocalDateTime.of(2026, 6, 15, 11, 0),
                LocalDateTime.of(2026, 6, 15, 9, 50)
            )
        )));

    mockMvc.perform(get("/api/v1/rooms/ranking"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data[0].score").value(1.23))
        .andExpect(jsonPath("$.data[0].room.roomId").value(10))
        .andExpect(jsonPath("$.data[0].room.topicId").value(1))
        .andExpect(jsonPath("$.data[0].room.title").value("인기 토론방"))
        .andExpect(jsonPath("$.data[0].room.status").value("OPEN"));

    verify(roomRankingService).getRankedOpenRooms();
  }
}