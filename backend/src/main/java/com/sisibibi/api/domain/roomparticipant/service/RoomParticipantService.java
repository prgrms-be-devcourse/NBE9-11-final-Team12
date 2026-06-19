package com.sisibibi.api.domain.roomparticipant.service;


import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantChangedEvent;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantEventPayload;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantEventType;
import com.sisibibi.api.domain.roomparticipant.dto.response.RoomParticipantCountRes;
import com.sisibibi.api.domain.roomparticipant.dto.response.RoomParticipantRes;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipant;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomParticipantService {

  private final RoomRepository roomRepository;
  private final RoomParticipantRepository roomParticipantRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public RoomParticipantRes joinRoom(Long roomId, Long userId) {

    LocalDateTime now = LocalDateTime.now();

    Room room = roomRepository.findByIdForUpdate(roomId)
        .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

    if (room.getStatus() != RoomStatus.OPEN) {
      throw new CustomException(ErrorCode.ROOM_CLOSED);
    }

    if (room.getEndedAt() != null && !room.getEndedAt().isAfter(now)) {
      throw new CustomException(ErrorCode.ROOM_CLOSED);
    }

    if (!room.isJoinableAt(LocalDateTime.now())) {
      throw new CustomException(ErrorCode.ROOM_CLOSED);
    }

    RoomParticipant participant = roomParticipantRepository
        .findByRoomIdAndUserId(roomId, userId)
        .map(existingParticipant -> {
          if (existingParticipant.getStatus() == RoomParticipantStatus.JOINED) {
            throw new CustomException(ErrorCode.ROOM_ALREADY_PARTICIPATED);
          }

          validateRoomCapacity(roomId, room);

          existingParticipant.rejoin();
          return existingParticipant;
        })
        .orElseGet(() -> roomParticipantRepository.save(
            RoomParticipant.join(roomId, userId)
        ));

    log.info(
        "Room participant joined. roomId={}, userId={}, participantId={}, status={}",
        roomId,
        userId,
        participant.getId(),
        participant.getStatus()
    );

    publishRoomParticipantChangedEvent(
        RoomParticipantEventType.PARTICIPANT_JOINED,
        roomId,
        userId
    );

    return RoomParticipantRes.from(participant);
  }

  @Transactional
  public void leaveRoom(Long roomId, Long userId) {
    if (!roomRepository.existsById(roomId)) {
      throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
    }

    RoomParticipant participant = roomParticipantRepository
        .findByRoomIdAndUserId(roomId, userId)
        .orElseThrow(() -> new CustomException(ErrorCode.ROOM_PARTICIPANT_NOT_FOUND));

    boolean wasJoined = participant.getStatus() == RoomParticipantStatus.JOINED;
    participant.leave();
    log.info(
        "Room participant left. roomId={}, userId={}, participantId={}",
        roomId,
        userId,
        participant.getId()
    );

    if (wasJoined) {
      publishRoomParticipantChangedEvent(
          RoomParticipantEventType.PARTICIPANT_LEFT,
          roomId,
          userId
      );
    }
  }
  public List<RoomParticipantRes> getRoomParticipants(Long roomId) {
    if (!roomRepository.existsById(roomId)) {
      throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
    }

    return roomParticipantRepository
        .findByRoomIdAndStatusOrderByJoinedAtAsc(roomId, RoomParticipantStatus.JOINED)
        .stream()
        .map(RoomParticipantRes::from)
        .toList();
  }

  public RoomParticipantCountRes getCurrentParticipantCount(Long roomId) {
    if (!roomRepository.existsById(roomId)) {
      throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
    }

    int participantCount = roomParticipantRepository.countByRoomIdAndStatus(
        roomId,
        RoomParticipantStatus.JOINED
    );

    return new RoomParticipantCountRes(roomId, participantCount);
  }

  private void publishRoomParticipantChangedEvent(
      RoomParticipantEventType type,
      Long roomId,
      Long userId
  ) {
    int participantCount = roomParticipantRepository.countByRoomIdAndStatus(
        roomId,
        RoomParticipantStatus.JOINED
    );
    eventPublisher.publishEvent(new RoomParticipantChangedEvent(
        type,
        roomId,
        RoomParticipantEventPayload.of(roomId, userId, participantCount)
    ));
  }

  private void validateRoomCapacity(Long roomId, Room room) {
    int participantCount = roomParticipantRepository.countByRoomIdAndStatus(
        roomId,
        RoomParticipantStatus.JOINED
    );

    if (participantCount >= room.getMaxParticipants()) {
      throw new CustomException(ErrorCode.ROOM_FULL);
    }
  }

}
