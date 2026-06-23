package com.sisibibi.api.domain.roomparticipant.service;

import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantChangedEvent;
import com.sisibibi.api.domain.roomparticipant.dto.event.RoomParticipantEventType;
import com.sisibibi.api.domain.roomparticipant.dto.response.RoomParticipantRes;
import com.sisibibi.api.domain.roomparticipant.dto.response.RoomParticipantCountRes;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipant;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import java.time.LocalDateTime;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoomParticipantServiceTest {

  @Mock
  private RoomRepository roomRepository;

  @Mock
  private RoomParticipantRepository roomParticipantRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @Mock
  private SpeakingQueueService speakingQueueService;

  @InjectMocks
  private RoomParticipantService roomParticipantService;

  @Test
  void joinRoom_savesParticipant_whenRoomIsOpen() {
    Room room = org.mockito.Mockito.mock(Room.class);
    given(room.getStatus()).willReturn(RoomStatus.OPEN);
    given(room.getMaxParticipants()).willReturn(100);
    given(room.isJoinableAt(any(LocalDateTime.class))).willReturn(true);
    given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));
    given(roomParticipantRepository.findByRoomIdAndUserId(1L, 2L)).willReturn(Optional.empty());
    given(roomParticipantRepository.save(any(RoomParticipant.class)))
        .willAnswer(invocation -> invocation.getArgument(0));
    given(roomParticipantRepository.countByRoomIdAndStatus(
        1L,
        RoomParticipantStatus.JOINED
    )).willReturn(3);

    RoomParticipantRes response = roomParticipantService.joinRoom(1L, 2L);

    ArgumentCaptor<RoomParticipant> captor = ArgumentCaptor.forClass(RoomParticipant.class);
    ArgumentCaptor<RoomParticipantChangedEvent> eventCaptor =
        ArgumentCaptor.forClass(RoomParticipantChangedEvent.class);
    verify(roomParticipantRepository).save(captor.capture());
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    RoomParticipant savedParticipant = captor.getValue();
    RoomParticipantChangedEvent event = eventCaptor.getValue();

    assertThat(savedParticipant.getRoomId()).isEqualTo(1L);
    assertThat(savedParticipant.getUserId()).isEqualTo(2L);
    assertThat(savedParticipant.getStatus()).isEqualTo(RoomParticipantStatus.JOINED);
    assertThat(savedParticipant.getJoinedAt()).isNotNull();

    assertThat(response.roomId()).isEqualTo(1L);
    assertThat(response.userId()).isEqualTo(2L);
    assertThat(response.status()).isEqualTo(RoomParticipantStatus.JOINED);

    assertThat(event.type()).isEqualTo(RoomParticipantEventType.PARTICIPANT_JOINED);
    assertThat(event.roomId()).isEqualTo(1L);
    assertThat(event.payload().roomId()).isEqualTo(1L);
    assertThat(event.payload().userId()).isEqualTo(2L);
    assertThat(event.payload().participantCount()).isEqualTo(3);
    assertThat(event.payload().occurredAt()).isNotNull();
  }

  @Test
  void joinRoom_throwsRoomNotFound_whenRoomDoesNotExist() {
    given(roomRepository.findByIdForUpdate(999L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> roomParticipantService.joinRoom(999L, 2L))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_NOT_FOUND);

    verify(roomParticipantRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void joinRoom_throwsRoomClosed_whenRoomIsClosed() {
    Room room = org.mockito.Mockito.mock(Room.class);
    given(room.getStatus()).willReturn(RoomStatus.CLOSED);
    given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));

    assertThatThrownBy(() -> roomParticipantService.joinRoom(1L, 2L))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_CLOSED);

    verify(roomParticipantRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void joinRoom_throwsAlreadyParticipated_whenUserAlreadyJoined() {
    Room room = org.mockito.Mockito.mock(Room.class);
    RoomParticipant participant = org.mockito.Mockito.mock(RoomParticipant.class);

    given(room.getStatus()).willReturn(RoomStatus.OPEN);
    given(room.getMaxParticipants()).willReturn(100);
    given(room.isJoinableAt(any(LocalDateTime.class))).willReturn(true);
    given(participant.getStatus()).willReturn(RoomParticipantStatus.JOINED);
    given(roomParticipantRepository.countByRoomIdAndStatus(
        1L,
        RoomParticipantStatus.JOINED
    )).willReturn(3);
    given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));
    given(roomParticipantRepository.findByRoomIdAndUserId(1L, 2L))
        .willReturn(Optional.of(participant));

    assertThatThrownBy(() -> roomParticipantService.joinRoom(1L, 2L))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_ALREADY_PARTICIPATED);

    verify(roomParticipantRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void joinRoom_rejoinsExistingParticipant_whenParticipantPreviouslyLeft() {
    Room room = org.mockito.Mockito.mock(Room.class);
    RoomParticipant participant = RoomParticipant.join(1L, 2L);
    participant.leave();

    given(room.getStatus()).willReturn(RoomStatus.OPEN);
    given(room.getMaxParticipants()).willReturn(100);
    given(room.isJoinableAt(any(LocalDateTime.class))).willReturn(true);
    given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));
    given(roomParticipantRepository.findByRoomIdAndUserId(1L, 2L))
        .willReturn(Optional.of(participant));
    given(roomParticipantRepository.countByRoomIdAndStatus(
        1L,
        RoomParticipantStatus.JOINED
    )).willReturn(2);

    RoomParticipantRes response = roomParticipantService.joinRoom(1L, 2L);

    ArgumentCaptor<RoomParticipantChangedEvent> eventCaptor =
        ArgumentCaptor.forClass(RoomParticipantChangedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    assertThat(participant.getStatus()).isEqualTo(RoomParticipantStatus.JOINED);
    assertThat(participant.getLeftAt()).isNull();
    assertThat(response.status()).isEqualTo(RoomParticipantStatus.JOINED);
    assertThat(eventCaptor.getValue().type())
        .isEqualTo(RoomParticipantEventType.PARTICIPANT_JOINED);
    assertThat(eventCaptor.getValue().payload().participantCount()).isEqualTo(2);
    verify(roomParticipantRepository, never()).save(any());
  }

  @Test
  void leaveRoom_changesParticipantStatusToLeft_whenParticipantJoined() {
    RoomParticipant participant = RoomParticipant.join(1L, 2L);

    given(roomRepository.existsById(1L)).willReturn(true);
    given(roomParticipantRepository.findByRoomIdAndUserId(1L, 2L))
        .willReturn(Optional.of(participant));
    given(roomParticipantRepository.countByRoomIdAndStatus(
        1L,
        RoomParticipantStatus.JOINED
    )).willReturn(1);

    roomParticipantService.leaveRoom(1L, 2L);

    ArgumentCaptor<RoomParticipantChangedEvent> eventCaptor =
        ArgumentCaptor.forClass(RoomParticipantChangedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());

    assertThat(participant.getStatus()).isEqualTo(RoomParticipantStatus.LEFT);
    assertThat(participant.getLeftAt()).isNotNull();
    assertThat(eventCaptor.getValue().type())
        .isEqualTo(RoomParticipantEventType.PARTICIPANT_LEFT);
    assertThat(eventCaptor.getValue().roomId()).isEqualTo(1L);
    assertThat(eventCaptor.getValue().payload().roomId()).isEqualTo(1L);
    assertThat(eventCaptor.getValue().payload().userId()).isEqualTo(2L);
    assertThat(eventCaptor.getValue().payload().participantCount()).isEqualTo(1);
    assertThat(eventCaptor.getValue().payload().occurredAt()).isNotNull();
    verify(speakingQueueService).completeCurrentSpeakerWhenParticipantLeft(1L, 2L);
  }

  @Test
  void leaveRoom_isIdempotent_whenParticipantAlreadyLeft() {
    RoomParticipant participant = RoomParticipant.join(1L, 2L);
    participant.leave();
    var firstLeftAt = participant.getLeftAt();

    given(roomRepository.existsById(1L)).willReturn(true);
    given(roomParticipantRepository.findByRoomIdAndUserId(1L, 2L))
        .willReturn(Optional.of(participant));

    roomParticipantService.leaveRoom(1L, 2L);

    assertThat(participant.getStatus()).isEqualTo(RoomParticipantStatus.LEFT);
    assertThat(participant.getLeftAt()).isEqualTo(firstLeftAt);
    verify(eventPublisher, never()).publishEvent(any());
    verify(speakingQueueService, never())
        .completeCurrentSpeakerWhenParticipantLeft(any(), any());
    verify(roomParticipantRepository, never()).countByRoomIdAndStatus(any(), any());
  }

  @Test
  void leaveRoom_throwsRoomNotFound_whenRoomDoesNotExist() {
    given(roomRepository.existsById(999L)).willReturn(false);

    assertThatThrownBy(() -> roomParticipantService.leaveRoom(999L, 2L))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_NOT_FOUND);

    verify(eventPublisher, never()).publishEvent(any());
    verify(speakingQueueService, never())
        .completeCurrentSpeakerWhenParticipantLeft(any(), any());
  }

  @Test
  void leaveRoom_throwsParticipantNotFound_whenUserHasNeverJoinedRoom() {
    given(roomRepository.existsById(1L)).willReturn(true);
    given(roomParticipantRepository.findByRoomIdAndUserId(1L, 2L))
        .willReturn(Optional.empty());

    assertThatThrownBy(() -> roomParticipantService.leaveRoom(1L, 2L))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_PARTICIPANT_NOT_FOUND);

    verify(eventPublisher, never()).publishEvent(any());
    verify(speakingQueueService, never())
        .completeCurrentSpeakerWhenParticipantLeft(any(), any());
  }

  @Test
  void getRoomParticipants_returnsJoinedParticipantsOrderedByJoinedAtAsc() {
    RoomParticipant firstParticipant = RoomParticipant.join(1L, 2L);
    RoomParticipant secondParticipant = RoomParticipant.join(1L, 3L);

    given(roomRepository.existsById(1L)).willReturn(true);
    given(roomParticipantRepository.findByRoomIdAndStatusOrderByJoinedAtAsc(
        1L,
        RoomParticipantStatus.JOINED
    )).willReturn(List.of(firstParticipant, secondParticipant));

    List<RoomParticipantRes> result = roomParticipantService.getRoomParticipants(1L);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).roomId()).isEqualTo(1L);
    assertThat(result.get(0).userId()).isEqualTo(2L);
    assertThat(result.get(0).status()).isEqualTo(RoomParticipantStatus.JOINED);
    assertThat(result.get(1).userId()).isEqualTo(3L);

    verify(roomRepository).existsById(1L);
    verify(roomParticipantRepository).findByRoomIdAndStatusOrderByJoinedAtAsc(
        1L,
        RoomParticipantStatus.JOINED
    );
  }

  @Test
  void getRoomParticipants_returnsEmptyList_whenJoinedParticipantDoesNotExist() {
    given(roomRepository.existsById(1L)).willReturn(true);
    given(roomParticipantRepository.findByRoomIdAndStatusOrderByJoinedAtAsc(
        1L,
        RoomParticipantStatus.JOINED
    )).willReturn(List.of());

    List<RoomParticipantRes> result = roomParticipantService.getRoomParticipants(1L);

    assertThat(result).isEmpty();

    verify(roomRepository).existsById(1L);
    verify(roomParticipantRepository).findByRoomIdAndStatusOrderByJoinedAtAsc(
        1L,
        RoomParticipantStatus.JOINED
    );
  }

  @Test
  void getRoomParticipants_throwsRoomNotFound_whenRoomDoesNotExist() {
    given(roomRepository.existsById(999L)).willReturn(false);

    assertThatThrownBy(() -> roomParticipantService.getRoomParticipants(999L))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_NOT_FOUND);

    verify(roomRepository).existsById(999L);
    verify(roomParticipantRepository, never())
        .findByRoomIdAndStatusOrderByJoinedAtAsc(any(), any());
  }

  @Test
  void getCurrentParticipantCount_returnsJoinedParticipantCount() {
    given(roomRepository.existsById(1L)).willReturn(true);
    given(roomParticipantRepository.countByRoomIdAndStatus(
        1L,
        RoomParticipantStatus.JOINED
    )).willReturn(3);

    RoomParticipantCountRes result =
        roomParticipantService.getCurrentParticipantCount(1L);

    assertThat(result.roomId()).isEqualTo(1L);
    assertThat(result.participantCount()).isEqualTo(3);
  }

  @Test
  void getCurrentParticipantCount_throwsRoomNotFound_whenRoomDoesNotExist() {
    given(roomRepository.existsById(999L)).willReturn(false);

    assertThatThrownBy(() ->
        roomParticipantService.getCurrentParticipantCount(999L)
    )
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_NOT_FOUND);

    verify(roomParticipantRepository, never()).countByRoomIdAndStatus(any(), any());
  }
}
