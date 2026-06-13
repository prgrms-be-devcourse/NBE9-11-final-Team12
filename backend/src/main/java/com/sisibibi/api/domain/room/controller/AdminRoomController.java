package com.sisibibi.api.domain.room.controller;

import com.sisibibi.api.domain.room.dto.request.CreateRoomReq;
import com.sisibibi.api.domain.room.dto.response.CreateRoomRes;
import com.sisibibi.api.domain.room.service.RoomService;
import com.sisibibi.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/rooms")
public class AdminRoomController {

  private final RoomService roomService;

  @PostMapping
  public ResponseEntity<ApiResponse<CreateRoomRes>> createRoom(
      @Valid @RequestBody CreateRoomReq request
  ) {
    CreateRoomRes result = roomService.createRoom(request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.created("토론방 생성이 완료되었습니다.", result));
  }
}
