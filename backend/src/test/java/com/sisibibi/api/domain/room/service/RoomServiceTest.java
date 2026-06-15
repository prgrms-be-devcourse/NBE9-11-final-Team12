package com.sisibibi.api.domain.room.service;

import com.sisibibi.api.domain.room.dto.request.CreateRoomReq;
import com.sisibibi.api.domain.room.dto.response.CreateRoomRes;
import com.sisibibi.api.domain.room.dto.response.RoomSummaryRes;
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

import java.time.LocalDateTime;
import java.util.List;
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
    assertThat(savedRoom.getCreatedAt()).isNull();
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

  @Test
  void getOpenRooms_returnsOnlyOpenRoomsOrderedByCreatedAtDesc() {
    Room firstRoom = Room.open(1L, "첫 번째 토론방");
    Room secondRoom = Room.open(2L, "두 번째 토론방");

    given(roomRepository.findByStatusOrderByCreatedAtDesc(RoomStatus.OPEN))
        .willReturn(List.of(secondRoom, firstRoom));

    List<RoomSummaryRes> result = roomService.getOpenRooms();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).title()).isEqualTo("두 번째 토론방");
    assertThat(result.get(1).title()).isEqualTo("첫 번째 토론방");

    verify(roomRepository).findByStatusOrderByCreatedAtDesc(RoomStatus.OPEN);
  }

  @Test
  void closeExpiredRooms_closesOpenRooms_whenEndedAtIsBeforeOrEqualNow() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 15, 12, 0);
    Room expiredRoom = Room.open(1L, "종료 대상 토론방");

    given(roomRepository.findByStatusAndEndedAtLessThanEqual(RoomStatus.OPEN, now))
        .willReturn(List.of(expiredRoom));

    int closedCount = roomService.closeExpiredRooms(now);

    assertThat(closedCount).isEqualTo(1);
    assertThat(expiredRoom.getStatus()).isEqualTo(RoomStatus.CLOSED);
    assertThat(expiredRoom.getEndedAt()).isEqualTo(now);

    verify(roomRepository).findByStatusAndEndedAtLessThanEqual(RoomStatus.OPEN, now);
  }

  @Test
  void closeExpiredRooms_returnsZero_whenExpiredRoomDoesNotExist() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 15, 12, 0);

    given(roomRepository.findByStatusAndEndedAtLessThanEqual(RoomStatus.OPEN, now))
        .willReturn(List.of());

    int closedCount = roomService.closeExpiredRooms(now);

    assertThat(closedCount).isZero();

    verify(roomRepository).findByStatusAndEndedAtLessThanEqual(RoomStatus.OPEN, now);
  }
}