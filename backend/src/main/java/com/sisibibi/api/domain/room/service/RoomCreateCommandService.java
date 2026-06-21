package com.sisibibi.api.domain.room.service;

import com.sisibibi.api.domain.room.dto.response.CreateRoomRes;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.entity.TopicStatus;
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RoomCreateCommandService {

  private final RoomRepository roomRepository;
  private final TopicRepository topicRepository;

  @Transactional
  public CreateRoomRes createRoom(Long topicId, String debateTitle, Integer maxParticipants) {
    Topic topic = topicRepository.findByIdForUpdate(topicId)
        .orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));

    if (topic.getStatus() != TopicStatus.APPROVED) {
      throw new CustomException(ErrorCode.TOPIC_NOT_APPROVED);
    }

    if (roomRepository.existsByTopicId(topic.getId())) {
      throw new CustomException(ErrorCode.ROOM_ALREADY_EXISTS);
    }

    LocalDateTime startedAt = LocalDateTime.now();
    LocalDateTime endedAt = startedAt.plusMinutes(5);
    int resolvedMaxParticipants = resolveMaxParticipants(maxParticipants);

    Room room = Room.open(
        topic.getId(),
        resolveTitle(debateTitle, topic.getTitle()),
        startedAt,
        endedAt,
        resolvedMaxParticipants
    );

    try {
      Room savedRoom = roomRepository.save(room);
      return CreateRoomRes.from(savedRoom);
    } catch (DataIntegrityViolationException e) {
      throw new CustomException(ErrorCode.ROOM_ALREADY_EXISTS);
    }
  }

  private String resolveTitle(String debateTitle, String fallbackTitle) {
    if (debateTitle == null || debateTitle.isBlank()) {
      return fallbackTitle;
    }

    return debateTitle.trim();
  }

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