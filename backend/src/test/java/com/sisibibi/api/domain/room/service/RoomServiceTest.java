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
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

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

  @Mock
  private ApplicationEventPublisher eventPublisher;

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
    LocalDateTime firstStartedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
    LocalDateTime firstEndedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
    LocalDateTime secondStartedAt = LocalDateTime.of(2026, 6, 15, 13, 0);
    LocalDateTime secondEndedAt = LocalDateTime.of(2026, 6, 15, 15, 0);

    Room firstRoom = Room.open(1L, "첫 번째 토론방", firstStartedAt, firstEndedAt);
    Room secondRoom = Room.open(2L, "두 번째 토론방", secondStartedAt, secondEndedAt);

    given(roomRepository.findByStatusOrderByCreatedAtDesc(RoomStatus.OPEN))
        .willReturn(List.of(secondRoom, firstRoom));

    List<RoomSummaryRes> result = roomService.getOpenRooms();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).title()).isEqualTo("두 번째 토론방");
    assertThat(result.get(1).title()).isEqualTo("첫 번째 토론방");

    verify(roomRepository).findByStatusOrderByCreatedAtDesc(RoomStatus.OPEN);
  }

  @Test
  void closeExpiredRooms_closesExpiredRoomsAndPublishesEvents() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 15, 12, 0);
    Room firstRoom = Room.open(
        1L,
        "첫 번째 토론방",
        LocalDateTime.of(2026, 6, 15, 11, 0),
        LocalDateTime.of(2026, 6, 15, 11, 5)
    );
    Room secondRoom = Room.open(
        2L,
        "두 번째 토론방",
        LocalDateTime.of(2026, 6, 15, 11, 10),
        LocalDateTime.of(2026, 6, 15, 11, 15)
    );

    given(roomRepository.findByStatusAndEndedAtLessThanEqual(
        eq(RoomStatus.OPEN),
        eq(now),
        any(Pageable.class)
    )).willReturn(List.of(firstRoom, secondRoom));

    int closedCount = roomService.closeExpiredRooms(now);

    assertThat(closedCount).isEqualTo(2);
    assertThat(firstRoom.getStatus()).isEqualTo(RoomStatus.CLOSED);
    assertThat(firstRoom.getEndedAt()).isEqualTo(now);
    assertThat(secondRoom.getStatus()).isEqualTo(RoomStatus.CLOSED);
    assertThat(secondRoom.getEndedAt()).isEqualTo(now);

    verify(roomRepository).findByStatusAndEndedAtLessThanEqual(
        eq(RoomStatus.OPEN),
        eq(now),
        any(Pageable.class)
    );
    verify(eventPublisher, times(2)).publishEvent(any(RoomClosedEvent.class));
  }

  @Test
  void closeExpiredRooms_returnsZeroAndDoesNotPublishEvent_whenExpiredRoomDoesNotExist() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 15, 12, 0);

    given(roomRepository.findByStatusAndEndedAtLessThanEqual(
        eq(RoomStatus.OPEN),
        eq(now),
        any(Pageable.class)
    )).willReturn(List.of());

    int closedCount = roomService.closeExpiredRooms(now);

    assertThat(closedCount).isZero();

    verify(roomRepository).findByStatusAndEndedAtLessThanEqual(
        eq(RoomStatus.OPEN),
        eq(now),
        any(Pageable.class)
    );
    verify(eventPublisher, never()).publishEvent(any(RoomClosedEvent.class));
  }
  @Test
  void getRooms_returnsRoomsOrderedByCreatedAtDesc() {
    LocalDateTime firstStartedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
    LocalDateTime firstEndedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
    LocalDateTime secondStartedAt = LocalDateTime.of(2026, 6, 15, 13, 0);
    LocalDateTime secondEndedAt = LocalDateTime.of(2026, 6, 15, 15, 0);

    Room firstRoom = Room.open(1L, "첫 번째 토론방", firstStartedAt, firstEndedAt);
    Room secondRoom = Room.open(2L, "두 번째 토론방", secondStartedAt, secondEndedAt);

    given(roomRepository.findAllByOrderByCreatedAtDesc())
        .willReturn(List.of(secondRoom, firstRoom));

    List<RoomSummaryRes> result = roomService.getRooms();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).title()).isEqualTo("두 번째 토론방");
    assertThat(result.get(1).title()).isEqualTo("첫 번째 토론방");

    verify(roomRepository).findAllByOrderByCreatedAtDesc();
  }

  @Test
  void getRoom_returnsRoomDetail_whenRoomExists() {
    LocalDateTime firstStartedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
    LocalDateTime firstEndedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
    Room room = Room.open(1L, "상세 조회 토론방", firstStartedAt, firstEndedAt);

    given(roomRepository.findById(10L)).willReturn(Optional.of(room));

    RoomDetailRes result = roomService.getRoom(10L);

    assertThat(result.topicId()).isEqualTo(1L);
    assertThat(result.title()).isEqualTo("상세 조회 토론방");
    assertThat(result.status()).isEqualTo(RoomStatus.OPEN);

    verify(roomRepository).findById(10L);
  }

  @Test
  void getRoom_throwsRoomNotFound_whenRoomDoesNotExist() {
    given(roomRepository.findById(999L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> roomService.getRoom(999L))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
  }

  @Test
  void updateRoom_updatesRoom_whenRoomExists() {
    LocalDateTime firstStartedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
    LocalDateTime firstEndedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
    Room room = Room.open(1L, "수정 전 제목", firstStartedAt, firstEndedAt);

    given(roomRepository.findById(10L)).willReturn(Optional.of(room));

    RoomDetailRes result = roomService.updateRoom(
        10L,
        new UpdateRoomReq("수정 후 제목", firstStartedAt, firstEndedAt)
    );

    assertThat(result.title()).isEqualTo("수정 후 제목");
    assertThat(result.startedAt()).isEqualTo(firstStartedAt);
    assertThat(result.endedAt()).isEqualTo(firstEndedAt);

    verify(roomRepository).findById(10L);
  }

  @Test
  void updateRoom_throwsRoomNotFound_whenRoomDoesNotExist() {
    given(roomRepository.findById(999L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> roomService.updateRoom(
        999L,
        new UpdateRoomReq("수정 후 제목", null, null)
    ))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
  }

  @Test
  void updateRoom_throwsInvalidInput_whenTitleIsBlank() {
    LocalDateTime firstStartedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
    LocalDateTime firstEndedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
    Room room = Room.open(1L, "수정 전 제목", firstStartedAt, firstEndedAt);

    given(roomRepository.findById(10L)).willReturn(Optional.of(room));

    assertThatThrownBy(() -> roomService.updateRoom(
        10L,
        new UpdateRoomReq("   ", null, null)
    ))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
  }

  @Test
  void updateRoom_throwsInvalidInput_whenEndedAtIsBeforeStartedAt() {
    LocalDateTime firstStartedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
    LocalDateTime firstEndedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
    Room room = Room.open(1L, "수정 후 제목", firstStartedAt, firstEndedAt);

    given(roomRepository.findById(10L)).willReturn(Optional.of(room));

    assertThatThrownBy(() -> roomService.updateRoom(
        10L,
        new UpdateRoomReq(
            "수정 후 제목",
            LocalDateTime.of(2026, 6, 15, 12, 0),
            LocalDateTime.of(2026, 6, 15, 10, 0)
        )
    ))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
  }

  @Test
  void deleteRoom_closesRoom_whenRoomExists() {
    LocalDateTime firstStartedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
    LocalDateTime firstEndedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
    Room room = Room.open(1L, "삭제 대상 토론방", firstStartedAt, firstEndedAt);

    given(roomRepository.findById(10L)).willReturn(Optional.of(room));

    roomService.deleteRoom(10L);

    assertThat(room.getStatus()).isEqualTo(RoomStatus.CLOSED);
    assertThat(room.getEndedAt()).isNotNull();

    verify(roomRepository).findById(10L);
  }

  @Test
  void deleteRoom_throwsRoomNotFound_whenRoomDoesNotExist() {
    given(roomRepository.findById(999L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> roomService.deleteRoom(999L))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_NOT_FOUND);

    verify(roomRepository).findById(999L);
  }
}