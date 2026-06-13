package com.sisibibi.api.domain.room.service;

import com.sisibibi.api.domain.room.dto.request.CreateRoomReq;
import com.sisibibi.api.domain.room.dto.response.CreateRoomRes;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

  @Mock
  private RoomRepository roomRepository;

  @Mock
  private TopicRepository topicRepository;

  @InjectMocks
  private RoomService roomService;

  @Test
  void createRoom_savesOpenRoom_whenTopicIsApproved() {
    Topic topic = Topic.approved("토론 주제", "설명", "IT", "https://example.com");

    given(topicRepository.findById(1L)).willReturn(Optional.of(topic));
    given(roomRepository.existsByTopicId(topic.getId())).willReturn(false);
    given(roomRepository.save(any(Room.class))).willAnswer(invocation -> invocation.getArgument(0));

    CreateRoomRes result = roomService.createRoom(new CreateRoomReq(1L));

    ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
    verify(roomRepository).save(captor.capture());

    Room savedRoom = captor.getValue();

    assertThat(savedRoom.getTopicId()).isEqualTo(topic.getId());
    assertThat(savedRoom.getTitle()).isEqualTo("토론 주제");
    assertThat(savedRoom.getStatus()).isEqualTo(RoomStatus.OPEN);
    assertThat(savedRoom.getStartedAt()).isNotNull();
    assertThat(savedRoom.getCreatedAt()).isNotNull();
    assertThat(result.status()).isEqualTo(RoomStatus.OPEN);
  }

  @Test
  void createRoom_throwsTopicNotFound_whenTopicDoesNotExist() {
    given(topicRepository.findById(999L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> roomService.createRoom(new CreateRoomReq(999L)))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.TOPIC_NOT_FOUND);

    verify(roomRepository, never()).save(any());
  }

  @Test
  void createRoom_throwsRoomAlreadyExists_whenTopicAlreadyHasRoom() {
    Topic topic = Topic.approved("토론 주제", "설명", "IT", "https://example.com");

    given(topicRepository.findById(1L)).willReturn(Optional.of(topic));
    given(roomRepository.existsByTopicId(topic.getId())).willReturn(true);

    assertThatThrownBy(() -> roomService.createRoom(new CreateRoomReq(1L)))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_ALREADY_EXISTS);

    verify(roomRepository, never()).save(any());
  }
}