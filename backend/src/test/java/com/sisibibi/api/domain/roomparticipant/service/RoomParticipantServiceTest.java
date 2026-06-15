package com.sisibibi.api.domain.roomparticipant.service;

import com.sisibibi.api.domain.roomparticipant.dto.response.RoomParticipantRes;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipant;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

  @InjectMocks
  private RoomParticipantService roomParticipantService;

  @Test
  void joinRoom_savesParticipant_whenRoomIsOpen() {
    Room room = org.mockito.Mockito.mock(Room.class);
    given(room.getStatus()).willReturn(RoomStatus.OPEN);
    given(roomRepository.findById(1L)).willReturn(Optional.of(room));
    given(roomParticipantRepository.findByRoomIdAndUserId(1L, 2L)).willReturn(Optional.empty());
    given(roomParticipantRepository.save(any(RoomParticipant.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    RoomParticipantRes response = roomParticipantService.joinRoom(1L, 2L);

    ArgumentCaptor<RoomParticipant> captor = ArgumentCaptor.forClass(RoomParticipant.class);
    verify(roomParticipantRepository).save(captor.capture());

    RoomParticipant savedParticipant = captor.getValue();

    assertThat(savedParticipant.getRoomId()).isEqualTo(1L);
    assertThat(savedParticipant.getUserId()).isEqualTo(2L);
    assertThat(savedParticipant.getStatus()).isEqualTo(RoomParticipantStatus.JOINED);
    assertThat(savedParticipant.getJoinedAt()).isNotNull();

    assertThat(response.roomId()).isEqualTo(1L);
    assertThat(response.userId()).isEqualTo(2L);
    assertThat(response.status()).isEqualTo(RoomParticipantStatus.JOINED);
  }

  @Test
  void joinRoom_throwsRoomNotFound_whenRoomDoesNotExist() {
    given(roomRepository.findById(999L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> roomParticipantService.joinRoom(999L, 2L))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_NOT_FOUND);

    verify(roomParticipantRepository, never()).save(any());
  }

  @Test
  void joinRoom_throwsRoomClosed_whenRoomIsClosed() {
    Room room = org.mockito.Mockito.mock(Room.class);
    given(room.getStatus()).willReturn(RoomStatus.CLOSED);
    given(roomRepository.findById(1L)).willReturn(Optional.of(room));

    assertThatThrownBy(() -> roomParticipantService.joinRoom(1L, 2L))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_CLOSED);

    verify(roomParticipantRepository, never()).save(any());
  }

  @Test
  void joinRoom_throwsAlreadyParticipated_whenUserAlreadyJoined() {
    Room room = org.mockito.Mockito.mock(Room.class);
    RoomParticipant participant = org.mockito.Mockito.mock(RoomParticipant.class);

    given(room.getStatus()).willReturn(RoomStatus.OPEN);
    given(participant.getStatus()).willReturn(RoomParticipantStatus.JOINED);
    given(roomRepository.findById(1L)).willReturn(Optional.of(room));
    given(roomParticipantRepository.findByRoomIdAndUserId(1L, 2L))
        .willReturn(Optional.of(participant));

    assertThatThrownBy(() -> roomParticipantService.joinRoom(1L, 2L))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_ALREADY_PARTICIPATED);

    verify(roomParticipantRepository, never()).save(any());
  }

  @Test
  void leaveRoom_changesParticipantStatusToLeft_whenParticipantJoined() {
    RoomParticipant participant = RoomParticipant.join(1L, 2L);

    given(roomRepository.existsById(1L)).willReturn(true);
    given(roomParticipantRepository.findByRoomIdAndUserId(1L, 2L))
        .willReturn(Optional.of(participant));

    roomParticipantService.leaveRoom(1L, 2L);

    assertThat(participant.getStatus()).isEqualTo(RoomParticipantStatus.LEFT);
    assertThat(participant.getLeftAt()).isNotNull();
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
  }

  @Test
  void leaveRoom_throwsRoomNotFound_whenRoomDoesNotExist() {
    given(roomRepository.existsById(999L)).willReturn(false);

    assertThatThrownBy(() -> roomParticipantService.leaveRoom(999L, 2L))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
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
  }
}