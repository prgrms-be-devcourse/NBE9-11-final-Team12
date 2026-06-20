package com.sisibibi.api.domain.room.service;

import com.sisibibi.api.domain.room.dto.event.RoomClosedEvent;
import com.sisibibi.api.domain.room.dto.request.CreateRoomReq;
import com.sisibibi.api.domain.room.dto.request.UpdateRoomReq;
import com.sisibibi.api.domain.room.dto.response.CreateRoomRes;
import com.sisibibi.api.domain.room.dto.response.RoomDetailRes;
import com.sisibibi.api.domain.room.dto.response.RoomSummaryRes;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.entity.TopicStatus;
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

  private final RoomRepository roomRepository;
  private final TopicRepository topicRepository;
  private final RoomCloseCommandService roomCloseCommandService;
  private final ApplicationEventPublisher eventPublisher;



  @Transactional
  public CreateRoomRes createRoom(CreateRoomReq request) {
    Topic topic = topicRepository.findByIdForUpdate(request.topicId())
        .orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));


    if (topic.getStatus() != TopicStatus.APPROVED) {
      throw new CustomException(ErrorCode.TOPIC_NOT_APPROVED);
    }

    if (roomRepository.existsByTopicId(topic.getId())) {
      throw new CustomException(ErrorCode.ROOM_ALREADY_EXISTS);
    }
    LocalDateTime startedAt = LocalDateTime.now();
    LocalDateTime endedAt = startedAt.plusMinutes(5);
    int maxParticipants = resolveMaxParticipants(request.maxParticipants());

    Room room = Room.open(topic.getId(), topic.getTitle(), startedAt, endedAt, maxParticipants);

    try {
      Room savedRoom = roomRepository.save(room);
      return CreateRoomRes.from(savedRoom);
    } catch (DataIntegrityViolationException e) {
      throw new CustomException(ErrorCode.ROOM_ALREADY_EXISTS);
    }
  }

  // 관리자 방 수정
  @Transactional
  public RoomDetailRes updateRoom(Long roomId, UpdateRoomReq request) {
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

    validateUpdateRoomRequest(room, request);

    room.update(
        request.title(),
        request.startedAt(),
        request.endedAt(),
        request.maxParticipants()
    );

    return RoomDetailRes.from(room);
  }

  public List<RoomSummaryRes> getOpenRooms() {
    return roomRepository.findByStatusOrderByCreatedAtDesc(RoomStatus.OPEN)
        .stream()
        .map(RoomSummaryRes::from)
        .toList();
  }

  public int closeExpiredRooms(LocalDateTime now) {
    List<Long> expiredRoomIds = roomRepository.findExpiredOpenRoomIds(
        RoomStatus.OPEN,
        now,
        PageRequest.of(0, 100)
    );

    int closedCount = 0;

    for (Long roomId : expiredRoomIds) {
      try {
        if (roomCloseCommandService.closeExpiredRoom(roomId, now)) {
          closedCount++;
        }
      } catch (Exception e) {
        log.error("Failed to close expired room. roomId={}", roomId, e);
      }
    }

    return closedCount;
  }

  // 하나의 토론방 상세 조회
  public RoomDetailRes getRoom(Long roomId) {
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

    return RoomDetailRes.from(room);
  }

  // 전체 토론방 조회
  public List<RoomSummaryRes> getRooms() {
    return roomRepository.findAllByOrderByCreatedAtDesc()
        .stream()
        .map(RoomSummaryRes::from)
        .toList();
  }

  // 시간 검증 로직
  private void validateUpdateRoomRequest(Room room, UpdateRoomReq request) {
    // 빈칸인 제목 예외처리
    if (request.title() != null && request.title().isBlank()) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }

    if (request.maxParticipants() != null && request.maxParticipants() <= 0) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }

    LocalDateTime nextStartedAt = request.startedAt() != null
        ? request.startedAt()
        : room.getStartedAt();

    LocalDateTime nextEndedAt = request.endedAt() != null
        ? request.endedAt()
        : room.getEndedAt();

    // 시작시간이 끝시간보다 앞서는 로직 예외처리
    if (nextStartedAt != null && nextEndedAt != null && nextEndedAt.isBefore(nextStartedAt)) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  @Transactional
  public void deleteRoom(Long roomId) {
    Room room = roomRepository.findByIdForUpdate(roomId)
        .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

    if (room.getStatus() == RoomStatus.CLOSED) {
      return;
    }

    room.close(LocalDateTime.now());
    publishRoomClosedEvent(room);
  }

  private void publishRoomClosedEvent(Room room) {
    eventPublisher.publishEvent(new RoomClosedEvent(room.getId(), room.getEndedAt()));
  }

  // 방 정원 검증 로직
  private int resolveMaxParticipants(Integer maxParticipants) {
    if (maxParticipants == null) {
      return 100;
    }

    if (maxParticipants <= 0) {
      throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
    }

    return maxParticipants;
  }

}
