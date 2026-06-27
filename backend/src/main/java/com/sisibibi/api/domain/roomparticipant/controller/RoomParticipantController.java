package com.sisibibi.api.domain.roomparticipant.controller;

import com.sisibibi.api.domain.roomparticipant.dto.response.RoomParticipantCountRes;
import com.sisibibi.api.domain.roomparticipant.dto.response.RoomParticipantRes;
import com.sisibibi.api.domain.roomparticipant.service.RoomParticipantService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.response.ApiResponse;
import com.sisibibi.api.global.security.AuthPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "토론방 참여자", description = "토론방 참여자 입장, 퇴장, 조회 API")
@RequestMapping("/api/v1/rooms/{roomId}/participants")
public class RoomParticipantController {

  private final RoomParticipantService roomParticipantService;

  @Operation(
      summary = "토론방 입장",
      description = "현재 로그인 사용자를 지정한 토론방의 참여자로 등록합니다."
  )
  @PostMapping
  public ResponseEntity<ApiResponse<RoomParticipantRes>> joinRoom(
      @PathVariable @Positive Long roomId,
      @AuthenticationPrincipal AuthPrincipal principal
  ) {


    RoomParticipantRes response = roomParticipantService.joinRoom(roomId, principal.userId());

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(ApiResponse.created("토론방 입장이 완료되었습니다.", response));
  }


  @Operation(
      summary = "토론방 퇴장",
      description = "현재 로그인 사용자를 지정한 토론방에서 퇴장 처리합니다."
  )
  @PostMapping("/out")
  public ResponseEntity<ApiResponse<Void>> leaveRoom(
      @PathVariable @Positive Long roomId,
      @AuthenticationPrincipal AuthPrincipal principal
  ) {

    roomParticipantService.leaveRoom(roomId, principal.userId());

    return ResponseEntity.ok(ApiResponse.okMessage("토론방 퇴장이 완료되었습니다."));
  }

  @Operation(
      summary = "토론방 참여자 목록 조회",
      description = "지정한 토론방의 현재 참여자 목록을 조회합니다."
  )
  @GetMapping
  public ResponseEntity<ApiResponse<List<RoomParticipantRes>>> getRoomParticipants(
      @PathVariable @Positive Long roomId
  ) {
    List<RoomParticipantRes> response = roomParticipantService.getRoomParticipants(roomId);

    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @Operation(
      summary = "토론방 참여자 수 조회",
      description = "지정한 토론방의 현재 참여자 수를 조회합니다."
  )
  @GetMapping("/count")
  public ResponseEntity<ApiResponse<RoomParticipantCountRes>> getCurrentParticipantCount(
      @PathVariable @Positive Long roomId
  ) {
    RoomParticipantCountRes response = roomParticipantService.getCurrentParticipantCount(roomId);

    return ResponseEntity.ok(ApiResponse.ok(response));
  }
}