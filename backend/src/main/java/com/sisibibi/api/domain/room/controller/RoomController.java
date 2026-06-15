package com.sisibibi.api.domain.room.controller;

import com.sisibibi.api.domain.room.dto.response.RoomDetailRes;
import com.sisibibi.api.domain.room.dto.response.RoomSummaryRes;
import com.sisibibi.api.domain.room.service.RoomService;
import com.sisibibi.api.global.response.ApiResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms")
public class RoomController {

  private final RoomService roomService;

  @GetMapping("/open")
  public ResponseEntity<ApiResponse<List<RoomSummaryRes>>> getOpenRooms() {
    List<RoomSummaryRes> response = roomService.getOpenRooms();

    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @GetMapping("/{roomId}")
  public ResponseEntity<ApiResponse<RoomDetailRes>> getRoom(
      @PathVariable @Positive Long roomId
  ) {
    RoomDetailRes response = roomService.getRoom(roomId);

    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}