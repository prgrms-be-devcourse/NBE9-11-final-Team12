package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.dto.SpeechCreateCommand;
import com.sisibibi.api.domain.speech.dto.SpeechCreateResponse;
import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpeechServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomParticipantRepository roomParticipantRepository;

    @Mock
    private SpeechRepository speechRepository;

    @InjectMocks
    private SpeechService speechService;

    @Test
    void createMainOpinion_savesReadySpeech_whenRoomIsOpenAndUserIsParticipating() {
        Long roomId = 1L;
        Long userId = 2L;
        Room room = org.mockito.Mockito.mock(Room.class);
        given(room.getStatus()).willReturn(RoomStatus.OPEN);
        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                roomId,
                userId,
                RoomParticipantStatus.JOINED
        )).willReturn(true);
        given(speechRepository.save(org.mockito.ArgumentMatchers.any(Speech.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        SpeechCreateResponse response = speechService.createMainOpinion(
                roomId,
                userId,
                new SpeechCreateCommand("근거가 있는 찬성 의견입니다.", SpeechStance.PRO)
        );

        ArgumentCaptor<Speech> speechCaptor = ArgumentCaptor.forClass(Speech.class);
        verify(speechRepository).save(speechCaptor.capture());
        Speech savedSpeech = speechCaptor.getValue();

        assertThat(savedSpeech.getRoomId()).isEqualTo(roomId);
        assertThat(savedSpeech.getUserId()).isEqualTo(userId);
        assertThat(savedSpeech.getContent()).isEqualTo("근거가 있는 찬성 의견입니다.");
        assertThat(savedSpeech.getStance()).isEqualTo(SpeechStance.PRO);
        assertThat(savedSpeech.getStatus()).isEqualTo(SpeechStatus.READY);
        assertThat(response.status()).isEqualTo(SpeechStatus.READY);
    }

    @Test
    void createMainOpinion_throwsRoomNotFound_whenRoomDoesNotExist() {
        given(roomRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> speechService.createMainOpinion(
                1L,
                2L,
                new SpeechCreateCommand("의견", SpeechStance.CON)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    void createMainOpinion_throwsRoomClosed_whenRoomIsClosed() {
        Room room = org.mockito.Mockito.mock(Room.class);
        given(room.getStatus()).willReturn(RoomStatus.CLOSED);
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));

        assertThatThrownBy(() -> speechService.createMainOpinion(
                1L,
                2L,
                new SpeechCreateCommand("의견", SpeechStance.PRO)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_CLOSED);
    }

    @Test
    void createMainOpinion_throwsParticipationRequired_whenUserIsNotParticipating() {
        Room room = org.mockito.Mockito.mock(Room.class);
        given(room.getStatus()).willReturn(RoomStatus.OPEN);
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                1L,
                2L,
                RoomParticipantStatus.JOINED
        )).willReturn(false);

        assertThatThrownBy(() -> speechService.createMainOpinion(
                1L,
                2L,
                new SpeechCreateCommand("의견", SpeechStance.PRO)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_PARTICIPATION_REQUIRED);
    }
}
