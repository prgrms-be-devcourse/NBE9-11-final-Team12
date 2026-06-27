package com.sisibibi.api.domain.room.controller;

import com.sisibibi.api.domain.room.dto.request.CreateRoomReq;
import com.sisibibi.api.domain.room.dto.request.PreviewRoomTitleReq;
import com.sisibibi.api.domain.room.dto.request.UpdateRoomReq;
import com.sisibibi.api.domain.room.dto.response.CreateRoomRes;
import com.sisibibi.api.domain.room.dto.response.PreviewRoomTitleRes;
import com.sisibibi.api.domain.room.dto.response.RoomDetailRes;
import com.sisibibi.api.domain.room.service.RoomService;
import com.sisibibi.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "토론방 관리자", description = "관리자 토론방 생성, 수정, 삭제 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/rooms")
public class AdminRoomController {

  private final RoomService roomService;

  @Operation(
      summary = "토론방 제목 미리보기",
      description = "관리자가 토론방 생성 전에 입력값을 기반으로 생성될 토론방 제목을 미리 확인합니다."
  )
  @PostMapping("/title-preview")
  public ResponseEntity<ApiResponse<PreviewRoomTitleRes>> previewRoomTitle(
      @Valid @RequestBody PreviewRoomTitleReq request
  ) {
    PreviewRoomTitleRes result = roomService.previewRoomTitle(request);

    return ResponseEntity.ok(ApiResponse.ok("토론방 제목 미리보기가 완료되었습니다.", result));
  }

  @Operation(
      summary = "토론방 생성",
      description = "관리자가 새로운 토론방을 생성합니다."
  )
  @PostMapping
  public ResponseEntity<ApiResponse<CreateRoomRes>> createRoom(
      @Valid @RequestBody CreateRoomReq request
  ) {
    CreateRoomRes result = roomService.createRoom(request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.created("토론방 생성이 완료되었습니다.", result));
  }

  @Operation(
      summary = "토론방 수정",
      description = "관리자가 지정한 토론방의 정보를 수정합니다."
  )
  @PatchMapping("/{roomId}")
  public ResponseEntity<ApiResponse<RoomDetailRes>> updateRoom(
      @PathVariable @Positive Long roomId,
      @RequestBody UpdateRoomReq request
  ) {
    RoomDetailRes response = roomService.updateRoom(roomId, request);

    return ResponseEntity.ok(ApiResponse.ok("토론방 수정이 완료되었습니다.", response));
  }

  @Operation(
      summary = "토론방 삭제",
      description = "관리자가 지정한 토론방을 삭제합니다."
  )
  @DeleteMapping("/{roomId}")
  public ResponseEntity<ApiResponse<Void>> deleteRoom(
      @PathVariable @Positive Long roomId
  ) {
    roomService.deleteRoom(roomId);

    return ResponseEntity.ok(ApiResponse.okMessage("토론방 삭제가 완료되었습니다."));
  }
}
