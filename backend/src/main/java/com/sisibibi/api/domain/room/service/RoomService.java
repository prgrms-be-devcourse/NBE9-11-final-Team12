package com.sisibibi.api.domain.room.service;

import com.sisibibi.api.domain.room.dto.request.CreateRoomReq;
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

    Room room = Room.open(topic.getId(), topic.getTitle());
    Room savedRoom = roomRepository.save(room);

    return CreateRoomRes.from(savedRoom);
  }

  public List<RoomSummaryRes> getOpenRooms() {
    return roomRepository.findByStatusOrderByCreatedAtDesc(RoomStatus.OPEN)
        .stream()
        .map(RoomSummaryRes::from)
        .toList();
  }

  @Transactional
  public int closeExpiredRooms(LocalDateTime now) {
    return roomRepository.closeExpiredRooms(now);
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
}