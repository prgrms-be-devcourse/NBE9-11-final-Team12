package com.sisibibi.api.domain.room.controller;

import com.sisibibi.api.domain.room.dto.request.CreateRoomReq;
import com.sisibibi.api.domain.room.dto.request.PreviewRoomTitleReq;
import com.sisibibi.api.domain.room.dto.request.UpdateRoomReq;
import com.sisibibi.api.domain.room.dto.response.CreateRoomRes;
import com.sisibibi.api.domain.room.dto.response.PreviewRoomTitleRes;
import com.sisibibi.api.domain.room.dto.response.RoomDetailRes;
import com.sisibibi.api.domain.room.service.RoomService;
import com.sisibibi.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/rooms")
public class AdminRoomController {

  private final RoomService roomService;

  @PostMapping("/title-preview")
  public ResponseEntity<ApiResponse<PreviewRoomTitleRes>> previewRoomTitle(
      @Valid @RequestBody PreviewRoomTitleReq request
  ) {
    PreviewRoomTitleRes result = roomService.previewRoomTitle(request);

    return ResponseEntity.ok(ApiResponse.ok("토론방 제목 미리보기가 완료되었습니다.", result));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<CreateRoomRes>> createRoom(
      @Valid @RequestBody CreateRoomReq request
  ) {
    CreateRoomRes result = roomService.createRoom(request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.created("토론방 생성이 완료되었습니다.", result));
  }

  @PatchMapping("/{roomId}")
  public ResponseEntity<ApiResponse<RoomDetailRes>> updateRoom(
      @PathVariable @Positive Long roomId,
      @RequestBody UpdateRoomReq request
  ) {
    RoomDetailRes response = roomService.updateRoom(roomId, request);

    return ResponseEntity.ok(ApiResponse.ok("토론방 수정이 완료되었습니다.", response));
  }

  @DeleteMapping("/{roomId}")
  public ResponseEntity<ApiResponse<Void>> deleteRoom(
      @PathVariable @Positive Long roomId
  ) {
    roomService.deleteRoom(roomId);

    return ResponseEntity.ok(ApiResponse.okMessage("토론방 삭제가 완료되었습니다."));
  }
}
