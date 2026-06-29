package com.sisibibi.api.domain.room.controller;

import com.sisibibi.api.domain.room.dto.response.RoomDetailRes;
import com.sisibibi.api.domain.room.dto.response.RoomSyncStateRes;
import com.sisibibi.api.domain.room.dto.response.RoomSummaryRes;
import com.sisibibi.api.domain.room.service.RoomService;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "토론방", description = "토론방 조회 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
public class RoomController {

  private final RoomService roomService;

  @Operation(
      summary = "진행 중인 토론방 목록 조회",
      description = "현재 진행 중인 토론방 목록을 조회합니다."
  )
  @GetMapping("/open")
  public ResponseEntity<ApiResponse<List<RoomSummaryRes>>> getOpenRooms() {
    List<RoomSummaryRes> response = roomService.getOpenRooms();

    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @Operation(
      summary = "토론방 상세 조회",
      description = "지정한 토론방의 상세 정보를 조회합니다."
  )
  @GetMapping("/{roomId}")
  public ResponseEntity<ApiResponse<RoomDetailRes>> getRoom(
      @PathVariable @Positive Long roomId
  ) {
    RoomDetailRes response = roomService.getRoom(roomId);

    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @Operation(
      summary = "?좊줎諛?화면 동기화 상태 조회",
      description = "재연결, 포커스 복귀, 새로고침 후 화면 모드와 참여 상태를 보정합니다."
  )
  @GetMapping("/{roomId}/sync-state")
  public ResponseEntity<ApiResponse<RoomSyncStateRes>> getRoomSyncState(
      @PathVariable @Positive Long roomId,
      @AuthenticationPrincipal AuthPrincipal principal
  ) {
    RoomSyncStateRes response = roomService.getRoomSyncState(roomId, principal.userId());

    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @Operation(
      summary = "토론방 목록 조회",
      description = "전체 토론방 목록을 조회합니다."
  )
  @GetMapping
  public ResponseEntity<ApiResponse<List<RoomSummaryRes>>> getRooms() {
    List<RoomSummaryRes> response = roomService.getRooms();

    return ResponseEntity.ok(ApiResponse.ok(response));
  }


}
