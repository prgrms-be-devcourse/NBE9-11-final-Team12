package com.sisibibi.api.domain.roomparticipant.controller;

import com.sisibibi.api.domain.roomparticipant.dto.response.RoomParticipantRes;
import com.sisibibi.api.domain.roomparticipant.service.RoomParticipantService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rooms/{roomId}/participants")
public class RoomParticipantController {

  private final RoomParticipantService roomParticipantService;

  @PostMapping
  public ResponseEntity<ApiResponse<RoomParticipantRes>> joinRoom(
      @PathVariable @Positive Long roomId,
      @AuthenticationPrincipal AuthPrincipal principal
  ) {
    if (principal == null) {
      throw new CustomException(ErrorCode.UNAUTHORIZED);
    }


    RoomParticipantRes response = roomParticipantService.joinRoom(roomId, principal.userId());

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.created("토론방 입장이 완료되었습니다.", response));
  }


  @PostMapping("/out")
  public ResponseEntity<ApiResponse<Void>> leaveRoom(
      @PathVariable @Positive Long roomId,
      @AuthenticationPrincipal AuthPrincipal principal
  ) {
    if (principal == null) {
      throw new CustomException(ErrorCode.UNAUTHORIZED);
    }

    roomParticipantService.leaveRoom(roomId, principal.userId());

    return ResponseEntity.ok(ApiResponse.okMessage("토론방 퇴장이 완료되었습니다."));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<RoomParticipantRes>>> getRoomParticipants(
      @PathVariable @Positive Long roomId
  ) {
    List<RoomParticipantRes> response = roomParticipantService.getRoomParticipants(roomId);

    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}