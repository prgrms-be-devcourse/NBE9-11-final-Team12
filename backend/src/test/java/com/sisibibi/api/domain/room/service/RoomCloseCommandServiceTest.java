package com.sisibibi.api.domain.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.room.dto.event.RoomClosedEvent;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoomCloseCommandServiceTest {

  @Mock
  private RoomRepository roomRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private SpeakingQueueService speakingQueueService;

  @InjectMocks
  private RoomCloseCommandService roomCloseCommandService;

  @Test
  void closeRoom_closesRoomAndSpeakingQueues_whenRoomIsOpen() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 24, 12, 0);
    Room room = Room.open(
        1L,
        "토론방",
        LocalDateTime.of(2026, 6, 24, 10, 0),
        LocalDateTime.of(2026, 6, 24, 13, 0),
        100
    );
    ReflectionTestUtils.setField(room, "id", 1L);
    given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));

    boolean closed = roomCloseCommandService.closeRoom(1L, now);

    assertThat(closed).isTrue();
    assertThat(room.getStatus()).isEqualTo(RoomStatus.CLOSED);
    assertThat(room.getEndedAt()).isEqualTo(now);
    verify(speakingQueueService).closeSpeakingQueuesWhenRoomClosed(1L, now);
    ArgumentCaptor<RoomClosedEvent> eventCaptor =
        ArgumentCaptor.forClass(RoomClosedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().roomId()).isEqualTo(1L);
    assertThat(eventCaptor.getValue().closedAt()).isEqualTo(now);
  }

  @Test
  void closeRoom_doesNotCloseSpeakingQueues_whenRoomIsAlreadyClosed() {
    LocalDateTime closedAt = LocalDateTime.of(2026, 6, 24, 12, 0);
    Room room = Room.open(
        1L,
        "이미 닫힌 토론방",
        LocalDateTime.of(2026, 6, 24, 10, 0),
        LocalDateTime.of(2026, 6, 24, 13, 0),
        100
    );
    room.close(closedAt);
    given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));

    boolean closed = roomCloseCommandService.closeRoom(1L, closedAt.plusMinutes(1));

    assertThat(closed).isFalse();
    verify(speakingQueueService, never()).closeSpeakingQueuesWhenRoomClosed(anyLong(), any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void closeRoom_throwsRoomNotFound_whenRoomDoesNotExist() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 24, 12, 0);
    given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> roomCloseCommandService.closeRoom(1L, now))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_NOT_FOUND);

    verify(speakingQueueService, never()).closeSpeakingQueuesWhenRoomClosed(anyLong(), any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void closeExpiredRoom_closesRoomAndSpeakingQueues_whenRoomIsOpen() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 24, 12, 0);
    given(roomRepository.closeExpiredRoomIfOpen(
        1L,
        RoomStatus.OPEN,
        RoomStatus.CLOSED,
        now,
        now
    )).willReturn(1);

    boolean closed = roomCloseCommandService.closeExpiredRoom(1L, now);

    assertThat(closed).isTrue();
    verify(speakingQueueService).closeSpeakingQueuesWhenRoomClosed(1L, now);
    ArgumentCaptor<RoomClosedEvent> eventCaptor =
        ArgumentCaptor.forClass(RoomClosedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    assertThat(eventCaptor.getValue().roomId()).isEqualTo(1L);
    assertThat(eventCaptor.getValue().closedAt()).isEqualTo(now);
  }

  @Test
  void closeExpiredRoom_doesNotCloseSpeakingQueues_whenRoomWasNotUpdated() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 24, 12, 0);
    given(roomRepository.closeExpiredRoomIfOpen(
        1L,
        RoomStatus.OPEN,
        RoomStatus.CLOSED,
        now,
        now
    )).willReturn(0);

    boolean closed = roomCloseCommandService.closeExpiredRoom(1L, now);

    assertThat(closed).isFalse();
    verify(speakingQueueService, never()).closeSpeakingQueuesWhenRoomClosed(anyLong(), any());
    verify(eventPublisher, never()).publishEvent(any());
  }
}
