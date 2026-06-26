package com.sisibibi.api.domain.room.service;

import com.sisibibi.api.domain.room.config.RoomTopicGenerator;
import com.sisibibi.api.domain.room.dto.request.CreateRoomReq;
import com.sisibibi.api.domain.room.dto.request.PreviewRoomTitleReq;
import com.sisibibi.api.domain.room.dto.request.UpdateRoomReq;
import com.sisibibi.api.domain.room.dto.response.CreateRoomRes;
import com.sisibibi.api.domain.room.dto.response.PreviewRoomTitleRes;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
  private final RoomCreateCommandService roomCreateCommandService;
  private final RoomTopicGenerator roomTopicGenerator;



  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public CreateRoomRes createRoom(CreateRoomReq request) {
    Topic topic = topicRepository.findByIdAndStatus(request.topicId(), TopicStatus.APPROVED)
        .orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));

    if (roomRepository.existsByTopicId(topic.getId())) {
      throw new CustomException(ErrorCode.ROOM_ALREADY_EXISTS);
    }

    return roomCreateCommandService.createRoom(
        topic.getId(),
        request.title(),
        request.maxParticipants()
    );
  }

  public PreviewRoomTitleRes previewRoomTitle(PreviewRoomTitleReq request) {
    Topic topic = topicRepository.findByIdAndStatus(request.topicId(), TopicStatus.APPROVED)
        .orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));

    if (roomRepository.existsByTopicId(topic.getId())) {
      throw new CustomException(ErrorCode.ROOM_ALREADY_EXISTS);
    }

    String debateTitle = roomTopicGenerator.generate(topic);

    return new PreviewRoomTitleRes(topic.getId(), debateTitle);
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

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void deleteRoom(Long roomId) {
    roomCloseCommandService.closeRoom(roomId, LocalDateTime.now());
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
