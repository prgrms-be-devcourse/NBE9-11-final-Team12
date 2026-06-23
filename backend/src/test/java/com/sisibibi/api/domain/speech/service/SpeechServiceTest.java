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
import com.sisibibi.api.domain.speechreaction.repository.SpeechReactionRepository;
import com.sisibibi.api.domain.speechreaction.repository.projection.SpeechReactionSummaryProjection;
import com.sisibibi.api.domain.usersanction.service.UserSanctionPolicyService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import com.sisibibi.api.global.moderation.ProfanityDetector;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpeechServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomParticipantRepository roomParticipantRepository;

    @Mock
    private SpeechRepository speechRepository;

    @Mock
    private SpeechReactionRepository speechReactionRepository;

    @Mock
    private ProfanityDetector profanityDetector;

    @Mock
    private UserSanctionPolicyService userSanctionPolicyService;

    @InjectMocks
    private SpeechService speechService;

    @Test
    void createMainOpinion_throwsSpeechRestricted_whenUserHasActiveSanction() {
        doThrow(new CustomException(ErrorCode.USER_SPEECH_RESTRICTED))
                .when(userSanctionPolicyService)
                .validateSpeechAllowed(2L);

        assertThatThrownBy(() -> speechService.createMainOpinion(
                1L,
                2L,
                new SpeechCreateCommand("의견", SpeechStance.PRO)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_SPEECH_RESTRICTED);

        verify(speechRepository, never()).save(org.mockito.ArgumentMatchers.any(Speech.class));
    }

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
    void createMainOpinion_throwsProfanityDetected_whenContentContainsProfanity() {
        Room room = org.mockito.Mockito.mock(Room.class);
        given(room.getStatus()).willReturn(RoomStatus.OPEN);
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                1L,
                2L,
                RoomParticipantStatus.JOINED
        )).willReturn(true);
        given(profanityDetector.containsProfanity("욕설이 포함된 의견"))
                .willReturn(true);

        assertThatThrownBy(() -> speechService.createMainOpinion(
                1L,
                2L,
                new SpeechCreateCommand("욕설이 포함된 의견", SpeechStance.PRO)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_CONTENT_CONTAINS_PROFANITY);

        verify(speechRepository, never()).save(org.mockito.ArgumentMatchers.any(Speech.class));
    }

    @Test
    void getSpeeches_returnsRoomSpeechesInRepositoryOrder() {
        Long roomId = 1L;
        Long userId = 30L;
        Speech first = mockSpeech(2L, roomId, 10L, "최신 의견", SpeechStance.PRO,
                SpeechStatus.READY, LocalDateTime.of(2026, 6, 12, 12, 0));
        Speech second = mockSpeech(1L, roomId, 20L, "이전 의견", SpeechStance.CON,
                SpeechStatus.COMPLETED, LocalDateTime.of(2026, 6, 12, 11, 0));
        Speech next = org.mockito.Mockito.mock(Speech.class);
        SpeechReactionSummaryProjection firstSummary = reactionSummary(2L, 3L, 1L);
        given(roomRepository.existsById(roomId)).willReturn(true);
        given(speechRepository.findByRoomIdBeforeCursor(
                roomId,
                null,
                PageRequest.of(0, 3)
        )).willReturn(List.of(first, second, next));
        given(speechReactionRepository.findReactionSummaries(
                List.of(2L, 1L),
                userId
        )).willReturn(List.of(firstSummary));

        SpeechCursorPageRes response = speechService.getSpeeches(roomId, userId, null, 2);

        assertThat(response.items()).extracting(SpeechListRes::speechId).containsExactly(2L, 1L);
        assertThat(response.items()).extracting(SpeechListRes::content)
                .containsExactly("최신 의견", "이전 의견");
        assertThat(response.items()).extracting(SpeechListRes::reactionCount)
                .containsExactly(3L, 0L);
        assertThat(response.items()).extracting(SpeechListRes::reactedByMe)
                .containsExactly(true, false);
        assertThat(response.nextCursor()).isEqualTo(1L);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void getSpeeches_throwsRoomNotFound_whenRoomDoesNotExist() {
        given(roomRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> speechService.getSpeeches(1L, 2L, null, 20))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    void getSpeech_returnsSpeechDetail() {
        Long userId = 20L;
        Speech speech = mockSpeech(3L, 1L, 2L, "상세 의견", SpeechStance.PRO,
                SpeechStatus.SPEAKING, LocalDateTime.of(2026, 6, 12, 12, 30));
        given(speech.getLinkUrl()).willReturn("https://example.com/evidence");
        given(speech.getImageUrl()).willReturn("https://example.com/image.png");
        given(speech.getUpdatedAt()).willReturn(LocalDateTime.of(2026, 6, 12, 12, 30));
        SpeechReactionSummaryProjection summary = reactionSummary(3L, 5L, 1L);
        given(speechRepository.findByIdAndDeletedFalse(3L)).willReturn(Optional.of(speech));
        given(speechReactionRepository.findReactionSummaries(List.of(3L), userId))
                .willReturn(List.of(summary));

        SpeechDetailRes response = speechService.getSpeech(3L, userId);

        assertThat(response.speechId()).isEqualTo(3L);
        assertThat(response.linkUrl()).isEqualTo("https://example.com/evidence");
        assertThat(response.imageUrl()).isEqualTo("https://example.com/image.png");
        assertThat(response.status()).isEqualTo(SpeechStatus.SPEAKING);
        assertThat(response.reactionCount()).isEqualTo(5L);
        assertThat(response.reactedByMe()).isTrue();
    }

    @Test
    void getSpeech_throwsSpeechNotFound_whenSpeechDoesNotExist() {
        given(speechRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> speechService.getSpeech(1L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_NOT_FOUND);
    }

    @Test
    void updateSpeech_updatesOwnEditableSpeech() {
        Speech speech = Speech.createMainOpinion(1L, 2L, "기존 의견", SpeechStance.CON);
        given(speechRepository.findByIdAndDeletedFalse(3L)).willReturn(Optional.of(speech));
        givenOpenRoom(1L);

        SpeechDetailRes response = speechService.updateSpeech(
                3L,
                2L,
                new SpeechUpdateCommand("수정된 의견", SpeechStance.PRO)
        );

        assertThat(response.content()).isEqualTo("수정된 의견");
        assertThat(response.stance()).isEqualTo(SpeechStance.PRO);
        assertThat(response.updatedAt()).isNull();
    }

    @Test
    void updateSpeech_throwsSpeechNotFound_whenSpeechDoesNotExist() {
        given(speechRepository.findByIdAndDeletedFalse(3L)).willReturn(Optional.empty());

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
        given(speechRepository.findByIdAndDeletedFalse(3L)).willReturn(Optional.of(speech));

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
        given(speechRepository.findByIdAndDeletedFalse(3L)).willReturn(Optional.of(speech));

        assertThatThrownBy(() -> speechService.updateSpeech(
                3L,
                2L,
                new SpeechUpdateCommand("수정", SpeechStance.PRO)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_NOT_EDITABLE);
    }

    @Test
    void updateSpeech_throwsProfanityDetected_whenContentContainsProfanity() {
        Speech speech = Speech.createMainOpinion(1L, 2L, "기존 의견", SpeechStance.CON);
        given(speechRepository.findByIdAndDeletedFalse(3L)).willReturn(Optional.of(speech));
        givenOpenRoom(1L);
        given(profanityDetector.containsProfanity("욕설이 포함된 수정 의견"))
                .willReturn(true);

        assertThatThrownBy(() -> speechService.updateSpeech(
                3L,
                2L,
                new SpeechUpdateCommand("욕설이 포함된 수정 의견", SpeechStance.PRO)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_CONTENT_CONTAINS_PROFANITY);

        assertThat(speech.getContent()).isEqualTo("기존 의견");
        assertThat(speech.getStance()).isEqualTo(SpeechStance.CON);
    }

    @Test
    void updateSpeech_throwsRoomClosed_whenRoomIsClosed() {
        Speech speech = Speech.createMainOpinion(1L, 2L, "기존 의견", SpeechStance.CON);
        Room room = org.mockito.Mockito.mock(Room.class);
        given(speechRepository.findByIdAndDeletedFalse(3L)).willReturn(Optional.of(speech));
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(room.getStatus()).willReturn(RoomStatus.CLOSED);

        assertThatThrownBy(() -> speechService.updateSpeech(
                3L,
                2L,
                new SpeechUpdateCommand("수정", SpeechStance.PRO)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_CLOSED);
    }

    @Test
    void deleteSpeech_softDeletesOwnEditableSpeech() {
        Speech speech = Speech.createMainOpinion(1L, 2L, "삭제할 의견", SpeechStance.PRO);
        given(speechRepository.findByIdAndDeletedFalse(3L)).willReturn(Optional.of(speech));
        givenOpenRoom(1L);

        speechService.deleteSpeech(3L, 2L);

        assertThat(speech.isDeleted()).isTrue();
        assertThat(speech.getDeletedAt()).isNotNull();
    }

    @Test
    void deleteSpeech_throwsSpeechNotFound_whenSpeechDoesNotExist() {
        given(speechRepository.findByIdAndDeletedFalse(3L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> speechService.deleteSpeech(3L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_NOT_FOUND);
    }

    @Test
    void deleteSpeech_throwsAccessDenied_whenSpeechOwnerDoesNotMatch() {
        Speech speech = org.mockito.Mockito.mock(Speech.class);
        given(speech.getUserId()).willReturn(9L);
        given(speechRepository.findByIdAndDeletedFalse(3L)).willReturn(Optional.of(speech));

        assertThatThrownBy(() -> speechService.deleteSpeech(3L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_ACCESS_DENIED);
    }

    @Test
    void deleteSpeech_throwsNotEditable_whenSpeechAlreadyCompleted() {
        Speech speech = org.mockito.Mockito.mock(Speech.class);
        given(speech.getUserId()).willReturn(2L);
        given(speech.getStatus()).willReturn(SpeechStatus.COMPLETED);
        given(speechRepository.findByIdAndDeletedFalse(3L)).willReturn(Optional.of(speech));

        assertThatThrownBy(() -> speechService.deleteSpeech(3L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_NOT_EDITABLE);
    }

    @Test
    void deleteSpeech_throwsRoomClosed_whenRoomIsClosed() {
        Speech speech = Speech.createMainOpinion(1L, 2L, "삭제할 의견", SpeechStance.PRO);
        Room room = org.mockito.Mockito.mock(Room.class);
        given(speechRepository.findByIdAndDeletedFalse(3L)).willReturn(Optional.of(speech));
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(room.getStatus()).willReturn(RoomStatus.CLOSED);

        assertThatThrownBy(() -> speechService.deleteSpeech(3L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_CLOSED);

        assertThat(speech.isDeleted()).isFalse();
    }

    @Test
    void updateSpeechLink_updatesOwnEditableSpeech() {
        Speech speech = Speech.createMainOpinion(1L, 2L, "의견", SpeechStance.PRO);
        given(speechRepository.findByIdAndDeletedFalse(3L)).willReturn(Optional.of(speech));
        givenOpenRoom(1L);

        SpeechDetailRes response = speechService.updateSpeechLink(
                3L,
                2L,
                "https://example.com/evidence"
        );

        assertThat(response.linkUrl()).isEqualTo("https://example.com/evidence");
        assertThat(speech.getLinkUrl()).isEqualTo("https://example.com/evidence");
    }

    @Test
    void updateSpeechLink_throwsSpeechNotFound_whenSpeechDoesNotExist() {
        given(speechRepository.findByIdAndDeletedFalse(3L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> speechService.updateSpeechLink(
                3L,
                2L,
                "https://example.com/evidence"
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_NOT_FOUND);
    }

    @Test
    void updateSpeechLink_throwsAccessDenied_whenSpeechOwnerDoesNotMatch() {
        Speech speech = org.mockito.Mockito.mock(Speech.class);
        given(speech.getUserId()).willReturn(9L);
        given(speechRepository.findByIdAndDeletedFalse(3L)).willReturn(Optional.of(speech));

        assertThatThrownBy(() -> speechService.updateSpeechLink(
                3L,
                2L,
                "https://example.com/evidence"
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_ACCESS_DENIED);
    }

    @Test
    void updateSpeechLink_throwsNotEditable_whenSpeechAlreadyCompleted() {
        Speech speech = org.mockito.Mockito.mock(Speech.class);
        given(speech.getUserId()).willReturn(2L);
        given(speech.getStatus()).willReturn(SpeechStatus.COMPLETED);
        given(speechRepository.findByIdAndDeletedFalse(3L)).willReturn(Optional.of(speech));

        assertThatThrownBy(() -> speechService.updateSpeechLink(
                3L,
                2L,
                "https://example.com/evidence"
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEECH_NOT_EDITABLE);
    }

    @Test
    void updateSpeechLink_throwsRoomClosed_whenRoomIsClosed() {
        Speech speech = Speech.createMainOpinion(1L, 2L, "의견", SpeechStance.PRO);
        Room room = org.mockito.Mockito.mock(Room.class);
        given(speechRepository.findByIdAndDeletedFalse(3L)).willReturn(Optional.of(speech));
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(room.getStatus()).willReturn(RoomStatus.CLOSED);

        assertThatThrownBy(() -> speechService.updateSpeechLink(
                3L,
                2L,
                "https://example.com/evidence"
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_CLOSED);
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

    private void givenOpenRoom(Long roomId) {
        Room room = org.mockito.Mockito.mock(Room.class);
        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(room.getStatus()).willReturn(RoomStatus.OPEN);
    }

    private SpeechReactionSummaryProjection reactionSummary(
            Long speechId,
            long reactionCount,
            long myReactionCount
    ) {
        SpeechReactionSummaryProjection summary =
                org.mockito.Mockito.mock(SpeechReactionSummaryProjection.class);
        given(summary.getSpeechId()).willReturn(speechId);
        given(summary.getReactionCount()).willReturn(reactionCount);
        given(summary.getMyReactionCount()).willReturn(myReactionCount);
        return summary;
    }
}
