package com.sisibibi.api.domain.room.service;

import com.sisibibi.api.domain.room.config.RoomTopicGenerator;
import com.sisibibi.api.domain.room.dto.event.RoomClosedEvent;
import com.sisibibi.api.domain.room.dto.request.CreateRoomReq;
import com.sisibibi.api.domain.room.dto.request.UpdateRoomReq;
import com.sisibibi.api.domain.room.dto.response.CreateRoomRes;
import com.sisibibi.api.domain.room.dto.response.RoomDetailRes;
import com.sisibibi.api.domain.room.dto.response.RoomSummaryRes;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.entity.TopicStatus;
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

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

  @Mock
  private RoomCloseCommandService roomCloseCommandService;

  @Mock
  private RoomCreateCommandService roomCreateCommandService;

  @Mock
  private RoomTopicGenerator roomTopicGenerator;

  @Mock
  private SpeakingQueueService speakingQueueService;

  @Test
  void createRoom_createsRoomWithAiGeneratedTitle_whenTopicIsApproved() {
    Topic topic = Topic.approved("토론 주제", "설명", "IT", "https://example.com");
    ReflectionTestUtils.setField(topic, "id", 1L);

    CreateRoomRes response = new CreateRoomRes(
        10L,
        1L,
        "AI가 만든 토론방 제목",
        RoomStatus.OPEN,
        LocalDateTime.of(2026, 6, 21, 12, 0),
        null
    );

    given(topicRepository.findByIdAndStatus(1L, TopicStatus.APPROVED))
        .willReturn(Optional.of(topic));
    given(roomRepository.existsByTopicId(1L)).willReturn(false);
    given(roomTopicGenerator.generate(topic)).willReturn("AI가 만든 토론방 제목");
    given(roomCreateCommandService.createRoom(1L, "AI가 만든 토론방 제목", null))
        .willReturn(response);

    CreateRoomRes result = roomService.createRoom(new CreateRoomReq(1L, null));

    assertThat(result.topicId()).isEqualTo(1L);
    assertThat(result.title()).isEqualTo("AI가 만든 토론방 제목");
    assertThat(result.status()).isEqualTo(RoomStatus.OPEN);

    verify(topicRepository).findByIdAndStatus(1L, TopicStatus.APPROVED);
    verify(roomRepository).existsByTopicId(1L);
    verify(roomTopicGenerator).generate(topic);
    verify(roomCreateCommandService).createRoom(1L, "AI가 만든 토론방 제목", null);
    verify(roomRepository, never()).save(any());
  }

  @Test
  void createRoom_throwsTopicNotFound_whenTopicDoesNotExist() {
    given(topicRepository.findByIdAndStatus(999L, TopicStatus.APPROVED))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> roomService.createRoom(new CreateRoomReq(999L, null)))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.TOPIC_NOT_FOUND);

    verify(roomRepository, never()).existsByTopicId(any());
    verify(roomTopicGenerator, never()).generate(any());
    verify(roomCreateCommandService, never()).createRoom(any(), any(), any());
  }

  @Test
  void createRoom_throwsRoomAlreadyExists_whenTopicAlreadyHasRoom() {
    Topic topic = Topic.approved("토론 주제", "설명", "IT", "https://example.com");
    ReflectionTestUtils.setField(topic, "id", 1L);

    given(topicRepository.findByIdAndStatus(1L, TopicStatus.APPROVED))
        .willReturn(Optional.of(topic));
    given(roomRepository.existsByTopicId(1L)).willReturn(true);

    assertThatThrownBy(() -> roomService.createRoom(new CreateRoomReq(1L, null)))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_ALREADY_EXISTS);

    verify(roomRepository).existsByTopicId(1L);
    verify(roomTopicGenerator, never()).generate(any());
    verify(roomCreateCommandService, never()).createRoom(any(), any(), any());
  }

  @Test
  void createRoom_throwsTopicNotFound_whenTopicIsNotApproved() {
    given(topicRepository.findByIdAndStatus(1L, TopicStatus.APPROVED))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> roomService.createRoom(new CreateRoomReq(1L, null)))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.TOPIC_NOT_FOUND);

    verify(roomRepository, never()).existsByTopicId(any());
    verify(roomTopicGenerator, never()).generate(any());
    verify(roomCreateCommandService, never()).createRoom(any(), any(), any());
  }

  @Test
  void getOpenRooms_returnsOnlyOpenRoomsOrderedByCreatedAtDesc() {
    LocalDateTime firstStartedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
    LocalDateTime firstEndedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
    LocalDateTime secondStartedAt = LocalDateTime.of(2026, 6, 15, 13, 0);
    LocalDateTime secondEndedAt = LocalDateTime.of(2026, 6, 15, 15, 0);

    Room firstRoom = Room.open(1L, "첫 번째 토론방", firstStartedAt, firstEndedAt, 100);
    Room secondRoom = Room.open(2L, "두 번째 토론방", secondStartedAt, secondEndedAt, 100);

    given(roomRepository.findByStatusOrderByCreatedAtDesc(RoomStatus.OPEN))
        .willReturn(List.of(secondRoom, firstRoom));

    List<RoomSummaryRes> result = roomService.getOpenRooms();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).title()).isEqualTo("두 번째 토론방");
    assertThat(result.get(1).title()).isEqualTo("첫 번째 토론방");

    verify(roomRepository).findByStatusOrderByCreatedAtDesc(RoomStatus.OPEN);
  }

  @Test
  void closeExpiredRooms_closesExpiredRooms() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 15, 12, 0);

    given(roomRepository.findExpiredOpenRoomIds(
        eq(RoomStatus.OPEN),
        eq(now),
        any(Pageable.class)
    )).willReturn(List.of(10L, 20L));

    given(roomCloseCommandService.closeExpiredRoom(10L, now)).willReturn(true);
    given(roomCloseCommandService.closeExpiredRoom(20L, now)).willReturn(true);

    int closedCount = roomService.closeExpiredRooms(now);

    assertThat(closedCount).isEqualTo(2);

    verify(roomRepository).findExpiredOpenRoomIds(
        eq(RoomStatus.OPEN),
        eq(now),
        any(Pageable.class)
    );
    verify(roomCloseCommandService).closeExpiredRoom(10L, now);
    verify(roomCloseCommandService).closeExpiredRoom(20L, now);
  }

  @Test
  void closeExpiredRooms_returnsZero_whenExpiredRoomDoesNotExist() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 15, 12, 0);

    given(roomRepository.findExpiredOpenRoomIds(
        eq(RoomStatus.OPEN),
        eq(now),
        any(Pageable.class)
    )).willReturn(List.of());

    int closedCount = roomService.closeExpiredRooms(now);

    assertThat(closedCount).isZero();

    verify(roomRepository).findExpiredOpenRoomIds(
        eq(RoomStatus.OPEN),
        eq(now),
        any(Pageable.class)
    );
    verify(roomCloseCommandService, never()).closeExpiredRoom(anyLong(), any());
  }
  @Test
  void getRooms_returnsRoomsOrderedByCreatedAtDesc() {
    LocalDateTime firstStartedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
    LocalDateTime firstEndedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
    LocalDateTime secondStartedAt = LocalDateTime.of(2026, 6, 15, 13, 0);
    LocalDateTime secondEndedAt = LocalDateTime.of(2026, 6, 15, 15, 0);

    Room firstRoom = Room.open(1L, "첫 번째 토론방", firstStartedAt, firstEndedAt, 100);
    Room secondRoom = Room.open(2L, "두 번째 토론방", secondStartedAt, secondEndedAt, 100);

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
    Room room = Room.open(1L, "상세 조회 토론방", firstStartedAt, firstEndedAt, 100);

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
    Room room = Room.open(1L, "수정 전 제목", firstStartedAt, firstEndedAt, 100);

    given(roomRepository.findById(10L)).willReturn(Optional.of(room));

    RoomDetailRes result = roomService.updateRoom(
        10L,
        new UpdateRoomReq("수정 후 제목", firstStartedAt, firstEndedAt, null)
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
        new UpdateRoomReq("수정 후 제목", null, null, null)
    ))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
  }

  @Test
  void updateRoom_throwsInvalidInput_whenTitleIsBlank() {
    LocalDateTime firstStartedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
    LocalDateTime firstEndedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
    Room room = Room.open(1L, "수정 전 제목", firstStartedAt, firstEndedAt, 100);

    given(roomRepository.findById(10L)).willReturn(Optional.of(room));

    assertThatThrownBy(() -> roomService.updateRoom(
        10L,
        new UpdateRoomReq("   ", null, null, null)
    ))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
  }

  @Test
  void updateRoom_throwsInvalidInput_whenEndedAtIsBeforeStartedAt() {
    LocalDateTime firstStartedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
    LocalDateTime firstEndedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
    Room room = Room.open(1L, "수정 후 제목", firstStartedAt, firstEndedAt, 100);

    given(roomRepository.findById(10L)).willReturn(Optional.of(room));

    assertThatThrownBy(() -> roomService.updateRoom(
        10L,
        new UpdateRoomReq(
            "수정 후 제목",
            LocalDateTime.of(2026, 6, 15, 12, 0),
            LocalDateTime.of(2026, 6, 15, 10, 0),
            null
        )
    ))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
  }

  @Test
  void updateRoom_preservesExistingValues_whenOptionalFieldsAreNull() {
    LocalDateTime startedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
    LocalDateTime endedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
    Room room = Room.open(1L, "기존 제목", startedAt, endedAt, 100);

    given(roomRepository.findById(10L)).willReturn(Optional.of(room));

    RoomDetailRes result = roomService.updateRoom(
        10L,
        new UpdateRoomReq(null, null, null, null)
    );

    assertThat(result.title()).isEqualTo("기존 제목");
    assertThat(result.startedAt()).isEqualTo(startedAt);
    assertThat(result.endedAt()).isEqualTo(endedAt);
  }

  @Test
  void updateRoom_acceptsMissingStartedAt_whenEndedAtIsProvided() {
    LocalDateTime endedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
    Room room = Room.open(1L, "기존 제목", null, null, 100);

    given(roomRepository.findById(10L)).willReturn(Optional.of(room));

    RoomDetailRes result = roomService.updateRoom(
        10L,
        new UpdateRoomReq(null, null, endedAt, null)
    );

    assertThat(result.startedAt()).isNull();
    assertThat(result.endedAt()).isEqualTo(endedAt);
  }

  @Test
  void deleteRoom_closesRoom_whenRoomExists() {
    LocalDateTime firstStartedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
    LocalDateTime firstEndedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
    Room room = Room.open(1L, "삭제 대상 토론방", firstStartedAt, firstEndedAt, 100);
    ReflectionTestUtils.setField(room, "id", 10L);

    given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));

    roomService.deleteRoom(10L);

    assertThat(room.getStatus()).isEqualTo(RoomStatus.CLOSED);
    assertThat(room.getEndedAt()).isNotNull();

    verify(roomRepository).findByIdForUpdate(10L);
    verify(speakingQueueService).closeSpeakingQueuesWhenRoomClosed(10L, room.getEndedAt());
    ArgumentCaptor<RoomClosedEvent> eventCaptor =
        ArgumentCaptor.forClass(RoomClosedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().roomId()).isEqualTo(10L);
    assertThat(eventCaptor.getValue().closedAt()).isEqualTo(room.getEndedAt());
  }

  @Test
  void deleteRoom_doesNotPublishEvent_whenRoomIsAlreadyClosed() {
    LocalDateTime closedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
    Room room = Room.open(
        1L,
        "이미 닫힌 토론방",
        LocalDateTime.of(2026, 6, 15, 10, 0),
        closedAt,
        100
    );
    room.close(closedAt);

    given(roomRepository.findByIdForUpdate(10L)).willReturn(Optional.of(room));

    roomService.deleteRoom(10L);

    verify(speakingQueueService, never()).closeSpeakingQueuesWhenRoomClosed(anyLong(), any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void deleteRoom_throwsRoomNotFound_whenRoomDoesNotExist() {
    given(roomRepository.findByIdForUpdate(999L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> roomService.deleteRoom(999L))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_NOT_FOUND);

    verify(roomRepository).findByIdForUpdate(999L);
  }
}
