package com.sisibibi.api.domain.roomranking.controller;

import com.sisibibi.api.domain.room.service.RoomService;
import com.sisibibi.api.domain.roomranking.dto.response.RoomRankingRes;
import com.sisibibi.api.domain.roomranking.service.RoomRankingService;
import com.sisibibi.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "토론방 순위", description = "토론방순위 조회 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
public class RoomRankingController {

  private final RoomService roomService;
  private final RoomRankingService roomRankingService;

  @Operation(
      summary = "토론방 순위 조회",
      description = "참여자 수, 채팅 메시지 수, 공감 수 기반으로 계산한 토론방 순위를 조회합니다."
  )
  @GetMapping("/ranking")
  public ResponseEntity<ApiResponse<List<RoomRankingRes>>> getRoomRanking() {
    List<RoomRankingRes> response = roomRankingService.getRankedOpenRooms();

    return ResponseEntity.ok(ApiResponse.ok(response));
  }


}
