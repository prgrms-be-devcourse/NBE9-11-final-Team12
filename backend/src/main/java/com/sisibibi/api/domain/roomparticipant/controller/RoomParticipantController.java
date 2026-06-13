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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}