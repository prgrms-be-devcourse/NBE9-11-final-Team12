package com.sisibibi.api.domain.room.service;

import com.sisibibi.api.domain.room.dto.request.CreateRoomReq;
import com.sisibibi.api.domain.room.dto.request.UpdateRoomReq;
import com.sisibibi.api.domain.room.dto.response.CreateRoomRes;
import com.sisibibi.api.domain.room.dto.response.RoomDetailRes;
import com.sisibibi.api.domain.room.dto.response.RoomSummaryRes;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.dto.event.RoomClosedEvent;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.entity.TopicStatus;
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

  private final RoomRepository roomRepository;
  private final TopicRepository topicRepository;
  private final ApplicationEventPublisher eventPublisher;



  @Transactional
  public CreateRoomRes createRoom(CreateRoomReq request) {
    Topic topic = topicRepository.findById(request.topicId())
        .orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));

    if (topic.getStatus() != TopicStatus.APPROVED) {
      throw new CustomException(ErrorCode.TOPIC_NOT_APPROVED);
    }

    if (roomRepository.existsByTopicId(topic.getId())) {
      throw new CustomException(ErrorCode.ROOM_ALREADY_EXISTS);
    }
    LocalDateTime startedAt = LocalDateTime.now();
    LocalDateTime endedAt = startedAt.plusMinutes(5);

    Room room = Room.open(topic.getId(), topic.getTitle(), startedAt, endedAt);
    Room savedRoom = roomRepository.save(room);

    return CreateRoomRes.from(savedRoom);
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
        request.endedAt()
    );

    return RoomDetailRes.from(room);
  }

  public List<RoomSummaryRes> getOpenRooms() {
    return roomRepository.findByStatusOrderByCreatedAtDesc(RoomStatus.OPEN)
        .stream()
        .map(RoomSummaryRes::from)
        .toList();
  }

  @Transactional
  public int closeExpiredRooms(LocalDateTime now) {
    List<Room> expiredRooms = roomRepository.findByStatusAndEndedAtLessThanEqual(
        RoomStatus.OPEN,
        now,
        PageRequest.of(0, 100)
    );

    for (Room room : expiredRooms) {
      room.close(now);
      eventPublisher.publishEvent(new RoomClosedEvent(room.getId()));
    }

    return expiredRooms.size();
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
    Room room = roomRepository.findById(roomId)
        .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

    room.close(LocalDateTime.now());

    eventPublisher.publishEvent(new RoomClosedEvent(room.getId()));
  }


}