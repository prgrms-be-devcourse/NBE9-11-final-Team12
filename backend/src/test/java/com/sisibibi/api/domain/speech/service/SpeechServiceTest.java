package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.dto.command.SpeechCreateCommand;
import com.sisibibi.api.domain.speech.dto.command.SpeechUpdateCommand;
import com.sisibibi.api.domain.speech.dto.response.SpeechCreateRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechCursorPageRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechDetailRes;
import com.sisibibi.api.domain.speech.dto.response.SpeechListRes;
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
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
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

        SpeechCreateRes response = speechService.createMainOpinion(
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

    @Test
    void getSpeeches_returnsRoomSpeechesInRepositoryOrder() {
        Long roomId = 1L;
        Speech first = mockSpeech(2L, roomId, 10L, "최신 의견", SpeechStance.PRO,
                SpeechStatus.READY, LocalDateTime.of(2026, 6, 12, 12, 0));
        Speech second = mockSpeech(1L, roomId, 20L, "이전 의견", SpeechStance.CON,
                SpeechStatus.COMPLETED, LocalDateTime.of(2026, 6, 12, 11, 0));
        Speech next = org.mockito.Mockito.mock(Speech.class);
        given(roomRepository.existsById(roomId)).willReturn(true);
        given(speechRepository.findByRoomIdBeforeCursor(
                roomId,
                null,
                PageRequest.of(0, 3)
        )).willReturn(List.of(first, second, next));

        SpeechCursorPageRes response = speechService.getSpeeches(roomId, null, 2);

        assertThat(response.items()).extracting(SpeechListRes::speechId).containsExactly(2L, 1L);
        assertThat(response.items()).extracting(SpeechListRes::content)
                .containsExactly("최신 의견", "이전 의견");
        assertThat(response.nextCursor()).isEqualTo(1L);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void getSpeeches_throwsRoomNotFound_whenRoomDoesNotExist() {
        given(roomRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> speechService.getSpeeches(1L, null, 20))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    void getSpeech_returnsSpeechDetail() {
        Speech speech = mockSpeech(3L, 1L, 2L, "상세 의견", SpeechStance.PRO,
                SpeechStatus.SPEAKING, LocalDateTime.of(2026, 6, 12, 12, 30));
        given(speech.getLinkUrl()).willReturn("https://example.com/evidence");
        given(speech.getImageUrl()).willReturn("https://example.com/image.png");
        given(speech.getUpdatedAt()).willReturn(LocalDateTime.of(2026, 6, 12, 12, 30));
        given(speechRepository.findById(3L)).willReturn(Optional.of(speech));

        SpeechDetailRes response = speechService.getSpeech(3L);

        assertThat(response.speechId()).isEqualTo(3L);
        assertThat(response.linkUrl()).isEqualTo("https://example.com/evidence");
        assertThat(response.imageUrl()).isEqualTo("https://example.com/image.png");
        assertThat(response.status()).isEqualTo(SpeechStatus.SPEAKING);
    }

    @Test
    void getSpeech_throwsSpeechNotFound_whenSpeechDoesNotExist() {
        given(speechRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> speechService.getSpeech(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_NOT_FOUND);
    }

    @Test
    void updateSpeech_updatesOwnEditableSpeech() {
        Speech speech = Speech.createMainOpinion(1L, 2L, "기존 의견", SpeechStance.CON);
        given(speechRepository.findById(3L)).willReturn(Optional.of(speech));

        SpeechDetailRes response = speechService.updateSpeech(
                3L,
                2L,
                new SpeechUpdateCommand("수정된 의견", SpeechStance.PRO)
        );

        assertThat(response.content()).isEqualTo("수정된 의견");
        assertThat(response.stance()).isEqualTo(SpeechStance.PRO);
        assertThat(response.updatedAt()).isAfterOrEqualTo(response.createdAt());
    }

    @Test
    void updateSpeech_throwsSpeechNotFound_whenSpeechDoesNotExist() {
        given(speechRepository.findById(3L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> speechService.updateSpeech(
                3L,
                2L,
                new SpeechUpdateCommand("수정", SpeechStance.PRO)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_NOT_FOUND);
    }

    @Test
    void updateSpeech_throwsAccessDenied_whenSpeechOwnerDoesNotMatch() {
        Speech speech = org.mockito.Mockito.mock(Speech.class);
        given(speech.getUserId()).willReturn(9L);
        given(speechRepository.findById(3L)).willReturn(Optional.of(speech));

        assertThatThrownBy(() -> speechService.updateSpeech(
                3L,
                2L,
                new SpeechUpdateCommand("수정", SpeechStance.PRO)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_ACCESS_DENIED);
    }

    @Test
    void updateSpeech_throwsNotEditable_whenSpeechAlreadyCompleted() {
        Speech speech = org.mockito.Mockito.mock(Speech.class);
        given(speech.getUserId()).willReturn(2L);
        given(speech.getStatus()).willReturn(SpeechStatus.COMPLETED);
        given(speechRepository.findById(3L)).willReturn(Optional.of(speech));

        assertThatThrownBy(() -> speechService.updateSpeech(
                3L,
                2L,
                new SpeechUpdateCommand("수정", SpeechStance.PRO)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_NOT_EDITABLE);
    }

    private Speech mockSpeech(
            Long speechId,
            Long roomId,
            Long userId,
            String content,
            SpeechStance stance,
            SpeechStatus status,
            LocalDateTime createdAt
    ) {
        Speech speech = org.mockito.Mockito.mock(Speech.class);
        given(speech.getId()).willReturn(speechId);
        given(speech.getRoomId()).willReturn(roomId);
        given(speech.getUserId()).willReturn(userId);
        given(speech.getContent()).willReturn(content);
        given(speech.getStance()).willReturn(stance);
        given(speech.getStatus()).willReturn(status);
        given(speech.getCreatedAt()).willReturn(createdAt);
        return speech;
    }
}
