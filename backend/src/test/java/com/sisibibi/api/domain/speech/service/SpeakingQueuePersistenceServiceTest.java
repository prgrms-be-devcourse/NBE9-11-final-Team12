package com.sisibibi.api.domain.speech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.entity.RoomQueueSequence;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;
import com.sisibibi.api.domain.speech.repository.RoomQueueSequenceRepository;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.repository.projection.CurrentSpeakerProjection;
import com.sisibibi.api.domain.speech.repository.projection.SpeakingRequestEligibilityProjection;
import com.sisibibi.api.domain.speech.util.SpeakingStreakPolicy;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.domain.usersanction.service.UserSanctionPolicyService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SpeakingQueuePersistenceServiceTest {

    private static final java.time.LocalDateTime ASSIGNED_AT =
            java.time.LocalDateTime.of(2026, 6, 12, 11, 31);
    private static final java.time.LocalDateTime EXPIRES_AT =
            java.time.LocalDateTime.of(2026, 6, 12, 11, 33);

    @Mock
    private SpeakingQueueRepository speakingQueueRepository;

    @Mock
    private RoomQueueSequenceRepository roomQueueSequenceRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomParticipantRepository roomParticipantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSanctionPolicyService userSanctionPolicyService;

    @Mock
    private SpeechRepository speechRepository;

    @Spy
    private SpeakingStreakPolicy speakingStreakPolicy;

    @InjectMocks
    private SpeakingQueuePersistenceService speakingQueuePersistenceService;

    private Room openRoom(Long topicId, String title) {
        return Room.open(
            topicId,
            title,
            LocalDateTime.of(2099, 6, 15, 10, 0),
            LocalDateTime.of(2099, 6, 15, 12, 0),
            100
        );
    }

    @Test
    void createWaitingRequest_persistsNeutralRequestWhenStanceIsMissing() {
        givenEligibility(1L, 7L, eligibility(
                1,
                1,
                1,
                0,
                0
        ));
        given(roomQueueSequenceRepository.issueNextQueueOrderIfRoomActive(eq(1L), any()))
                .willReturn(1);
        given(roomQueueSequenceRepository.findNextQueueOrderByRoomId(1L))
                .willReturn(Optional.of(2));
        given(speakingQueueRepository.insertWaitingRequest(
                eq(1L),
                eq(7L),
                eq(1),
                isNull(),
                eq(SpeakingQueueStatus.WAITING.name()),
                any()
        )).willReturn(1);

        SpeakingQueue saved = speakingQueuePersistenceService.createWaitingRequest(
                1L,
                7L,
                null
        );

        assertThat(saved.getQueueOrder()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(saved.getStance()).isNull();
        verify(speakingQueueRepository).findSpeakingRequestEligibility(
                eq(1L),
                eq(7L),
                any()
        );
        verify(roomQueueSequenceRepository).issueNextQueueOrderIfRoomActive(eq(1L), any());
        verify(speakingQueueRepository).insertWaitingRequest(
                eq(1L),
                eq(7L),
                eq(1),
                isNull(),
                eq(SpeakingQueueStatus.WAITING.name()),
                any()
        );
    }

    @Test
    void createWaitingRequest_throwsStageRestricted_whenUserHasActiveSanction() {
        givenEligibility(1L, 7L, eligibility(
                1,
                1,
                1,
                0,
                1
        ));

        assertThatThrownBy(() -> speakingQueuePersistenceService.createWaitingRequest(
                1L,
                7L,
                SpeechStance.PRO
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_STAGE_RESTRICTED);

        verify(speakingQueueRepository, never()).insertWaitingRequest(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void createWaitingRequest_persistsRequestWithNextRoomScopedOrder() {
        givenEligibility(1L, 7L, eligibility(
                1,
                1,
                1,
                0,
                0
        ));
        given(roomQueueSequenceRepository.issueNextQueueOrderIfRoomActive(eq(1L), any()))
                .willReturn(1);
        given(roomQueueSequenceRepository.findNextQueueOrderByRoomId(1L))
                .willReturn(Optional.of(2));
        given(speakingQueueRepository.insertWaitingRequest(
                eq(1L),
                eq(7L),
                eq(1),
                eq(SpeechStance.PRO.name()),
                eq(SpeakingQueueStatus.WAITING.name()),
                any()
        )).willReturn(1);

        SpeakingQueue saved =
                speakingQueuePersistenceService.createWaitingRequest(1L, 7L, SpeechStance.PRO);

        assertThat(saved.getQueueOrder()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(saved.getStance()).isEqualTo(SpeechStance.PRO);

        InOrder inOrder = inOrder(roomQueueSequenceRepository, speakingQueueRepository);
        inOrder.verify(roomQueueSequenceRepository).issueNextQueueOrderIfRoomActive(
                eq(1L),
                any()
        );
        inOrder.verify(roomQueueSequenceRepository).findNextQueueOrderByRoomId(1L);
        inOrder.verify(speakingQueueRepository).insertWaitingRequest(
                eq(1L),
                eq(7L),
                eq(1),
                eq(SpeechStance.PRO.name()),
                eq(SpeakingQueueStatus.WAITING.name()),
                any()
        );
    }

    @Test
    void createWaitingRequest_rejectsExistingActiveRequest() {
        givenEligibility(1L, 7L, eligibility(
                1,
                1,
                1,
                1,
                0
        ));

        assertThatThrownBy(() ->
                speakingQueuePersistenceService.createWaitingRequest(1L, 7L, SpeechStance.PRO))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS);

        verify(speakingQueueRepository, never()).insertWaitingRequest(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void createWaitingRequest_rejectsUserWhoIsNotJoinedParticipant() {
        givenEligibility(1L, 7L, eligibility(
                1,
                1,
                0,
                0,
                0
        ));

        assertThatThrownBy(() ->
                speakingQueuePersistenceService.createWaitingRequest(1L, 7L, SpeechStance.PRO))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_PARTICIPATION_REQUIRED);

        verify(speakingQueueRepository, never())
                .existsByRoomIdAndUserIdAndStatusIn(
                        1L,
                        7L,
                        List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
                );
        verify(speakingQueueRepository, never()).insertWaitingRequest(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void createWaitingRequest_rejectsMissingRoom() {
        givenEligibility(1L, 7L, eligibility(
                0,
                0,
                0,
                0,
                0
        ));

        assertThatThrownBy(() ->
                speakingQueuePersistenceService.createWaitingRequest(1L, 7L, SpeechStance.PRO))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);

        verify(speakingQueueRepository, never())
                .existsByRoomIdAndUserIdAndStatusIn(
                        1L,
                        7L,
                        List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
                );
        verify(speakingQueueRepository, never()).insertWaitingRequest(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void createWaitingRequest_rejectsNullEligibilityAsMissingRoom() {
        givenEligibility(1L, 7L, null);

        assertThatThrownBy(() ->
                speakingQueuePersistenceService.createWaitingRequest(1L, 7L, SpeechStance.PRO))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);

        verify(speakingQueueRepository, never()).insertWaitingRequest(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void createWaitingRequest_rejectsNullEligibilityFlagsAsMissingRoom() {
        givenEligibility(1L, 7L, eligibility(
                null,
                null,
                null,
                null,
                null
        ));

        assertThatThrownBy(() ->
                speakingQueuePersistenceService.createWaitingRequest(1L, 7L, SpeechStance.PRO))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);

        verify(speakingQueueRepository, never()).insertWaitingRequest(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void createWaitingRequest_rejectsEndedRoom() {
        givenEligibility(1L, 7L, eligibility(
                1,
                0,
                1,
                0,
                0
        ));

        assertThatThrownBy(() ->
                speakingQueuePersistenceService.createWaitingRequest(1L, 7L, SpeechStance.PRO))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_CLOSED);

        verify(roomParticipantRepository, never())
                .existsByRoomIdAndUserIdAndStatus(
                        1L,
                        7L,
                        RoomParticipantStatus.JOINED
                );
        verify(speakingQueueRepository, never()).insertWaitingRequest(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void cancelWaitingRequest_cancelsWaitingRequestAfterLockingRoom() {
        SpeakingQueue waiting = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        given(roomRepository.findByIdForUpdate(1L))
            .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        )).willReturn(Optional.of(waiting));

        SpeakingQueue canceled =
                speakingQueuePersistenceService.cancelWaitingRequest(1L, 7L);

        assertThat(canceled).isSameAs(waiting);
        assertThat(canceled.getStatus()).isEqualTo(SpeakingQueueStatus.CANCELED);
        assertThat(canceled.getCanceledAt()).isNotNull();
        assertThat(canceled.getActiveRequest()).isNull();

        InOrder order = inOrder(roomRepository, speakingQueueRepository);
        order.verify(roomRepository).findByIdForUpdate(1L);
        order.verify(speakingQueueRepository).findByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        );
    }

    @Test
    void cancelWaitingRequest_rejectsMissingActiveRequest() {
        given(roomRepository.findByIdForUpdate(1L))
            .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        )).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                speakingQueuePersistenceService.cancelWaitingRequest(1L, 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEAKING_REQUEST_NOT_FOUND);
    }

    @Test
    void cancelWaitingRequest_rejectsAssignedRequest() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        LocalDateTime firstStartedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        LocalDateTime firstEndedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
        given(roomRepository.findByIdForUpdate(1L))
            .willReturn(Optional.of(Room.open(1L, "토론방", firstStartedAt, firstEndedAt, 100)));
        given(speakingQueueRepository.findByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        )).willReturn(Optional.of(assigned));

        assertThatThrownBy(() ->
                speakingQueuePersistenceService.cancelWaitingRequest(1L, 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEAKING_REQUEST_NOT_CANCELABLE);

        assertThat(assigned.getStatus()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
    }

    @Test
    void cancelWaitingRequest_rejectsMissingRoomBeforeFindingRequest() {
        given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                speakingQueuePersistenceService.cancelWaitingRequest(1L, 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);

        verify(speakingQueueRepository, never()).findByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        );
    }

    @Test
    void findMyActiveRequest_returnsActiveRequest() {
        SpeakingQueue waiting = SpeakingQueue.waiting(
                1L,
                7L,
                3,
                SpeechStance.PRO,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        given(roomRepository.existsById(1L)).willReturn(true);
        given(speakingQueueRepository.findByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        )).willReturn(Optional.of(waiting));

        Optional<SpeakingQueue> found =
                speakingQueuePersistenceService.findMyActiveRequest(1L, 7L);

        assertThat(found).contains(waiting);
    }

    @Test
    void findMyActiveRequest_returnsEmptyWhenActiveRequestDoesNotExist() {
        given(roomRepository.existsById(1L)).willReturn(true);
        given(speakingQueueRepository.findByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        )).willReturn(Optional.empty());

        Optional<SpeakingQueue> found =
                speakingQueuePersistenceService.findMyActiveRequest(1L, 7L);

        assertThat(found).isEmpty();
    }

    @Test
    void validateRoomExists_passesWhenRoomExists() {
        given(roomRepository.existsById(1L)).willReturn(true);

        speakingQueuePersistenceService.validateRoomExists(1L);

        verify(roomRepository).existsById(1L);
    }

    @Test
    void validateRoomExists_rejectsMissingRoom() {
        given(roomRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> speakingQueuePersistenceService.validateRoomExists(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    void findNicknamesByUserIds_returnsEmptyMapWhenIdsAreEmpty() {
        assertThat(speakingQueuePersistenceService.findNicknamesByUserIds(List.of()))
                .isEmpty();

        verify(userRepository, never()).findAllById(any());
    }

    @Test
    void countWaitingRequests_delegatesToRepository() {
        given(speakingQueueRepository.countByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.WAITING
        )).willReturn(3L);

        long count = speakingQueuePersistenceService.countWaitingRequests(1L);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void countWaitingRequestsBefore_delegatesToRepository() {
        given(speakingQueueRepository.countByRoomIdAndStatusAndQueueOrderLessThan(
                1L,
                SpeakingQueueStatus.WAITING,
                10
        )).willReturn(2L);

        long count = speakingQueuePersistenceService.countWaitingRequestsBefore(1L, 10);

        assertThat(count).isEqualTo(2L);
    }

    @Test
    void findWaitingRequestsForRedisProjection_returnsWaitingRequestsInQueueOrder() {
        List<SpeakingQueue> waitingRequests = List.of(
                SpeakingQueue.waiting(
                        1L,
                        10L,
                        1,
                        SpeechStance.PRO,
                        LocalDateTime.of(2026, 6, 12, 11, 30)
                ),
                SpeakingQueue.waiting(
                        1L,
                        20L,
                        2,
                        SpeechStance.CON,
                        LocalDateTime.of(2026, 6, 12, 11, 31)
                )
        );
        given(speakingQueueRepository.findByRoomIdAndStatusOrderByQueueOrderAsc(
                1L,
                SpeakingQueueStatus.WAITING
        )).willReturn(waitingRequests);

        List<SpeakingQueue> found =
                speakingQueuePersistenceService.findWaitingRequestsForRedisProjection(1L);

        assertThat(found).isEqualTo(waitingRequests);
    }

    @Test
    void findWaitingRequestsForRedisReadFallback_delegatesToRepository() {
        List<SpeakingQueue> waitingRequests = List.of(SpeakingQueue.waiting(
                1L,
                10L,
                1,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        ));
        given(speakingQueueRepository.findWaitingPageForRedisReadFallback(
                1L,
                SpeakingQueueStatus.WAITING.name(),
                10,
                20
        )).willReturn(waitingRequests);

        List<SpeakingQueue> found =
                speakingQueuePersistenceService.findWaitingRequestsForRedisReadFallback(
                        1L,
                        10,
                        20
                );

        assertThat(found).isEqualTo(waitingRequests);
    }

    @Test
    void findWaitingStancesByUserIds_returnsEmptyMapWhenIdsAreEmpty() {
        assertThat(speakingQueuePersistenceService.findWaitingStancesByUserIds(
                1L,
                List.of()
        )).isEmpty();

        verify(speakingQueueRepository, never()).findByRoomIdAndUserIdInAndStatus(
                any(),
                any(),
                any()
        );
    }

    @Test
    void findWaitingStancesByUserIds_keepsFirstStanceWhenDuplicateUserExists() {
        SpeakingQueue first = SpeakingQueue.waiting(
                1L,
                10L,
                1,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        SpeakingQueue duplicate = SpeakingQueue.waiting(
                1L,
                10L,
                2,
                SpeechStance.CON,
                LocalDateTime.of(2026, 6, 12, 11, 31)
        );
        given(speakingQueueRepository.findByRoomIdAndUserIdInAndStatus(
                1L,
                List.of(10L),
                SpeakingQueueStatus.WAITING
        )).willReturn(List.of(first, duplicate));

        assertThat(speakingQueuePersistenceService.findWaitingStancesByUserIds(
                1L,
                List.of(10L)
        )).containsEntry(10L, SpeechStance.PRO);
    }

    @Test
    void findCurrentSpeakerForRedisProjection_returnsAssignedRequest() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                10L,
                1,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.of(assigned));

        Optional<SpeakingQueue> found =
                speakingQueuePersistenceService.findCurrentSpeakerForRedisProjection(1L);

        assertThat(found).contains(assigned);
    }

    @Test
    void validateCurrentSpeaker_returnsCurrentSpeakerWhenUserMatches() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.of(assigned));

        SpeakingQueue currentSpeaker =
                speakingQueuePersistenceService.validateCurrentSpeaker(1L, 7L);

        assertThat(currentSpeaker).isSameAs(assigned);
    }

    @Test
    void validateCurrentSpeaker_rejectsWhenCurrentSpeakerDoesNotExist() {
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.empty());

        assertThatThrownBy(() -> speakingQueuePersistenceService.validateCurrentSpeaker(1L, 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CURRENT_SPEAKER_NOT_FOUND);
    }

    @Test
    void validateCurrentSpeaker_rejectsDifferentUser() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.of(assigned));

        assertThatThrownBy(() -> speakingQueuePersistenceService.validateCurrentSpeaker(1L, 8L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void closeActiveRequestsByRoomId_cancelsWaitingAndCompletesAssignedRequests() {
        LocalDateTime closedAt = LocalDateTime.of(2026, 6, 24, 12, 0);
        SpeakingQueue waiting = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 24, 11, 50)
        );
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                8L,
                16,
                SpeechStance.CON,
                LocalDateTime.of(2026, 6, 24, 11, 51)
        );
        assign(assigned);
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatusInOrderByQueueOrderAsc(
                1L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        )).willReturn(List.of(waiting, assigned));

        SpeakingQueueRoomCloseResult result =
                speakingQueuePersistenceService.closeActiveRequestsByRoomId(1L, closedAt);

        assertThat(result.canceledRequests()).containsExactly(waiting);
        assertThat(result.completedRequests()).containsExactly(assigned);
        assertThat(waiting.getStatus()).isEqualTo(SpeakingQueueStatus.CANCELED);
        assertThat(waiting.getCanceledAt()).isEqualTo(closedAt);
        assertThat(waiting.getActiveRequest()).isNull();
        assertThat(assigned.getStatus()).isEqualTo(SpeakingQueueStatus.COMPLETED);
        assertThat(assigned.getActiveRequest()).isNull();
        verify(speechRepository).completeSpeakingSpeeches(
                1L,
                8L,
                SpeechStatus.SPEAKING,
                SpeechStatus.COMPLETED,
                closedAt
        );
    }

    @Test
    void findMyActiveRequest_rejectsMissingRoom() {
        given(roomRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() ->
                speakingQueuePersistenceService.findMyActiveRequest(1L, 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);

        verify(speakingQueueRepository, never())
                .findByRoomIdAndUserIdAndStatusIn(
                        1L,
                        7L,
                        List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
                );
    }

    @Test
    void assignNextSpeaker_assignsFirstWaitingRequestWhenCurrentSpeakerDoesNotExist() {
        SpeakingQueue firstWaiting = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        given(roomRepository.findByIdForUpdate(1L))
            .willReturn(Optional.of(openRoom(1L, "토론방")));
        givenJoined(1L, 7L);
        given(speakingQueueRepository.existsByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(false);
        given(speakingQueueRepository
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING
                ))
                .willReturn(Optional.of(firstWaiting));

        SpeakingQueueAssignmentResult result =
                speakingQueuePersistenceService.assignNextSpeaker(
                        1L,
                        ASSIGNED_AT,
                        EXPIRES_AT
                );

        assertThat(result.assignedRequest()).contains(firstWaiting);
        assertThat(result.canceledRequests()).isEmpty();
        assertThat(firstWaiting.getStatus()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(firstWaiting.getAssignedAt()).isEqualTo(ASSIGNED_AT);
        assertThat(firstWaiting.getExpiresAt()).isEqualTo(EXPIRES_AT);

        InOrder order = inOrder(roomRepository, speakingQueueRepository);
        order.verify(speakingQueueRepository)
                .existsByRoomIdAndStatus(1L, SpeakingQueueStatus.ASSIGNED);
        order.verify(roomRepository).findByIdForUpdate(1L);
        order.verify(speakingQueueRepository)
                .existsByRoomIdAndStatus(1L, SpeakingQueueStatus.ASSIGNED);
        order.verify(speakingQueueRepository)
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING
                );
        order.verify(speakingQueueRepository)
                .findTop3ByRoomIdAndStatusInAndStanceIsNotNullOrderByAssignedAtDesc(
                        1L,
                        List.of(SpeakingQueueStatus.COMPLETED)
                );
    }

    @Test
    void assignNextSpeaker_cancelsWaitingRequestAndAssignsNextWhenUserLeftRoom() {
        SpeakingQueue leftUserWaiting = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        SpeakingQueue joinedUserWaiting = SpeakingQueue.waiting(
                1L,
                8L,
                16,
                SpeechStance.CON,
                LocalDateTime.of(2026, 6, 12, 11, 31)
        );
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.existsByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(false);
        given(speakingQueueRepository
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING
                ))
                .willReturn(Optional.of(leftUserWaiting), Optional.of(joinedUserWaiting));
        given(roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                1L,
                7L,
                RoomParticipantStatus.JOINED
        )).willReturn(false);
        givenJoined(1L, 8L);

        SpeakingQueueAssignmentResult result =
                speakingQueuePersistenceService.assignNextSpeaker(
                        1L,
                        ASSIGNED_AT,
                        EXPIRES_AT
                );

        assertThat(result.assignedRequest()).contains(joinedUserWaiting);
        assertThat(result.canceledRequests()).containsExactly(leftUserWaiting);
        assertThat(leftUserWaiting.getStatus()).isEqualTo(SpeakingQueueStatus.CANCELED);
        assertThat(leftUserWaiting.getCanceledAt()).isEqualTo(ASSIGNED_AT);
        assertThat(joinedUserWaiting.getStatus()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
    }

    @Test
    void assignNextSpeaker_prioritizesOppositeStanceAfterThreeSameStanceAssignments() {
        SpeakingQueue firstWaiting = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        SpeakingQueue oppositeWaiting = SpeakingQueue.waiting(
                1L,
                8L,
                30,
                SpeechStance.CON,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 40)
        );
        ReflectionTestUtils.setField(firstWaiting, "id", 101L);
        ReflectionTestUtils.setField(oppositeWaiting, "id", 102L);
        given(roomRepository.findByIdForUpdate(1L))
            .willReturn(Optional.of(openRoom(1L, "토론방")));
        givenJoined(1L, 8L);
        given(speakingQueueRepository.existsByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(false);
        given(speakingQueueRepository
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING
                ))
                .willReturn(Optional.of(firstWaiting));
        given(speakingQueueRepository
                .findTop3ByRoomIdAndStatusInAndStanceIsNotNullOrderByAssignedAtDesc(
                        1L,
                        List.of(SpeakingQueueStatus.COMPLETED)
                ))
                .willReturn(List.of(
                        completedAssignment(10L, 10, SpeechStance.PRO, 11, 35),
                        completedAssignment(11L, 11, SpeechStance.PRO, 11, 30),
                        completedAssignment(12L, 12, SpeechStance.PRO, 11, 25)
                ));
        given(speakingQueueRepository
                .findFirstByRoomIdAndStatusAndStanceOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING,
                        SpeechStance.CON
                ))
                .willReturn(Optional.of(oppositeWaiting));

        SpeakingQueueAssignmentResult result =
                speakingQueuePersistenceService.assignNextSpeaker(
                        1L,
                        ASSIGNED_AT,
                        EXPIRES_AT
                );

        assertThat(result.assignedRequest()).contains(oppositeWaiting);
        assertThat(result.canceledRequests()).isEmpty();
        assertThat(result.balancedAssignment()).isTrue();
        assertThat(oppositeWaiting.getStatus()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        verify(speakingQueueRepository)
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING
                );
    }

    @Test
    void assignNextSpeaker_fallsBackToFirstWaitingWhenOppositeStanceDoesNotExist() {
        SpeakingQueue firstWaiting = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        given(roomRepository.findByIdForUpdate(1L))
            .willReturn(Optional.of(openRoom(1L, "토론방")));
        givenJoined(1L, 7L);
        given(speakingQueueRepository.existsByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(false);
        given(speakingQueueRepository
                .findTop3ByRoomIdAndStatusInAndStanceIsNotNullOrderByAssignedAtDesc(
                        1L,
                        List.of(SpeakingQueueStatus.COMPLETED)
                ))
                .willReturn(List.of(
                        completedAssignment(10L, 10, SpeechStance.PRO, 11, 35),
                        completedAssignment(11L, 11, SpeechStance.PRO, 11, 30),
                        completedAssignment(12L, 12, SpeechStance.PRO, 11, 25)
                ));
        given(speakingQueueRepository
                .findFirstByRoomIdAndStatusAndStanceOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING,
                        SpeechStance.CON
                ))
                .willReturn(Optional.empty());
        given(speakingQueueRepository
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING
                ))
                .willReturn(Optional.of(firstWaiting));

        SpeakingQueueAssignmentResult result =
                speakingQueuePersistenceService.assignNextSpeaker(
                        1L,
                        ASSIGNED_AT,
                        EXPIRES_AT
                );

        assertThat(result.assignedRequest()).contains(firstWaiting);
        assertThat(result.canceledRequests()).isEmpty();
        assertThat(firstWaiting.getStatus()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
    }

    @Test
    void assignNextSpeaker_usesFirstWaitingWhenRecentAssignmentsAreMixed() {
        SpeakingQueue firstWaiting = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        given(roomRepository.findByIdForUpdate(1L))
            .willReturn(Optional.of(openRoom(1L, "토론방")));
        givenJoined(1L, 7L);
        given(speakingQueueRepository.existsByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(false);
        given(speakingQueueRepository
                .findTop3ByRoomIdAndStatusInAndStanceIsNotNullOrderByAssignedAtDesc(
                        1L,
                        List.of(SpeakingQueueStatus.COMPLETED)
                ))
                .willReturn(List.of(
                        completedAssignment(10L, 10, SpeechStance.PRO, 11, 35),
                        completedAssignment(11L, 11, SpeechStance.CON, 11, 30),
                        completedAssignment(12L, 12, SpeechStance.PRO, 11, 25)
                ));
        given(speakingQueueRepository
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING
                ))
                .willReturn(Optional.of(firstWaiting));

        SpeakingQueueAssignmentResult result =
                speakingQueuePersistenceService.assignNextSpeaker(
                        1L,
                        ASSIGNED_AT,
                        EXPIRES_AT
                );

        assertThat(result.assignedRequest()).contains(firstWaiting);
        assertThat(result.canceledRequests()).isEmpty();
        verify(speakingQueueRepository, never())
                .findFirstByRoomIdAndStatusAndStanceOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING,
                        SpeechStance.CON
                );
    }

    @Test
    void assignNextSpeaker_doesNotAssignWhenCurrentSpeakerAlreadyExists() {
        SpeakingQueue waiting = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        given(speakingQueueRepository.existsByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(true);

        SpeakingQueueAssignmentResult result =
                speakingQueuePersistenceService.assignNextSpeaker(
                        1L,
                        ASSIGNED_AT,
                        EXPIRES_AT
                );

        assertThat(result.assignedRequest()).isEmpty();
        assertThat(result.canceledRequests()).isEmpty();
        assertThat(waiting.getStatus()).isEqualTo(SpeakingQueueStatus.WAITING);
        verify(roomRepository, never()).findByIdForUpdate(1L);
        verify(speakingQueueRepository, never())
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING
                );
    }

    @Test
    void assignNextSpeaker_returnsEmptyWhenWaitingQueueIsEmpty() {
        given(roomRepository.findByIdForUpdate(1L))
            .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.existsByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(false);
        given(speakingQueueRepository
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING
                ))
                .willReturn(Optional.empty());

        SpeakingQueueAssignmentResult result =
                speakingQueuePersistenceService.assignNextSpeaker(
                        1L,
                        ASSIGNED_AT,
                        EXPIRES_AT
                );

        assertThat(result.assignedRequest()).isEmpty();
        assertThat(result.canceledRequests()).isEmpty();
    }

    @Test
    void assignNextSpeaker_rejectsMissingRoomBeforeInspectingQueue() {
        given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                speakingQueuePersistenceService.assignNextSpeaker(
                        1L,
                        ASSIGNED_AT,
                        EXPIRES_AT
                ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);

        verify(speakingQueueRepository)
                .existsByRoomIdAndStatus(1L, SpeakingQueueStatus.ASSIGNED);
        verify(speakingQueueRepository, never())
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING
                );
    }

    @Test
    void assignNextSpeaker_returnsEmptyWhenRoomIsEnded() {
        Room endedRoom = Room.open(
                1L,
                "종료된 토론방",
                ASSIGNED_AT.minusHours(2),
                ASSIGNED_AT.minusMinutes(1),
                100
        );
        given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(endedRoom));

        SpeakingQueueAssignmentResult result =
                speakingQueuePersistenceService.assignNextSpeaker(
                        1L,
                        ASSIGNED_AT,
                        EXPIRES_AT
                );

        assertThat(result.assignedRequest()).isEmpty();
        assertThat(result.canceledRequests()).isEmpty();
        verify(speakingQueueRepository)
                .existsByRoomIdAndStatus(1L, SpeakingQueueStatus.ASSIGNED);
        verify(speakingQueueRepository, never())
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING
                );
    }

    @Test
    void completeCurrentSpeaker_completesAssignedRequestAfterLockingRoom() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        given(roomRepository.findByIdForUpdate(1L))
            .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.of(assigned));

        SpeakingQueue completed =
                speakingQueuePersistenceService.completeCurrentSpeaker(1L, 7L);

        assertThat(completed).isSameAs(assigned);
        assertThat(completed.getStatus()).isEqualTo(SpeakingQueueStatus.COMPLETED);
        assertThat(completed.getActiveRequest()).isNull();
        verify(speechRepository).completeSpeakingSpeeches(
                eq(1L),
                eq(7L),
                eq(SpeechStatus.SPEAKING),
                eq(SpeechStatus.COMPLETED),
                any(LocalDateTime.class)
        );

        InOrder order = inOrder(roomRepository, speakingQueueRepository);
        order.verify(roomRepository).findByIdForUpdate(1L);
        order.verify(speakingQueueRepository).findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        );
    }

    @Test
    void completeCurrentSpeaker_rejectsWhenCurrentSpeakerDoesNotExist() {
        given(roomRepository.findByIdForUpdate(1L))
            .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                speakingQueuePersistenceService.completeCurrentSpeaker(1L, 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CURRENT_SPEAKER_NOT_FOUND);
    }

    @Test
    void completeCurrentSpeaker_rejectsDifferentUser() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        given(roomRepository.findByIdForUpdate(1L))
            .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.of(assigned));

        assertThatThrownBy(() ->
                speakingQueuePersistenceService.completeCurrentSpeaker(1L, 8L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        assertThat(assigned.getStatus()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
    }

    @Test
    void completeCurrentSpeakerIfMatches_completesAssignedRequestWhenUserMatches() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.of(assigned));

        Optional<SpeakingQueue> completed =
                speakingQueuePersistenceService.completeCurrentSpeakerIfMatches(1L, 7L);

        assertThat(completed).contains(assigned);
        assertThat(assigned.getStatus()).isEqualTo(SpeakingQueueStatus.COMPLETED);
        assertThat(assigned.getActiveRequest()).isNull();
        verify(speechRepository).completeSpeakingSpeeches(
                eq(1L),
                eq(7L),
                eq(SpeechStatus.SPEAKING),
                eq(SpeechStatus.COMPLETED),
                any(LocalDateTime.class)
        );
    }

    @Test
    void completeCurrentSpeakerIfMatches_returnsEmptyWhenUserDoesNotMatch() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.of(assigned));

        Optional<SpeakingQueue> completed =
                speakingQueuePersistenceService.completeCurrentSpeakerIfMatches(1L, 8L);

        assertThat(completed).isEmpty();
        assertThat(assigned.getStatus()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
    }

    @Test
    void completeCurrentSpeakerIfMatches_returnsEmptyWhenCurrentSpeakerDoesNotExist() {
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.empty());

        Optional<SpeakingQueue> completed =
                speakingQueuePersistenceService.completeCurrentSpeakerIfMatches(1L, 7L);

        assertThat(completed).isEmpty();
    }

    @Test
    void findCurrentSpeaker_returnsAssignedSpeaker() {
        CurrentSpeakerProjection assigned = currentSpeakerProjection();
        given(roomRepository.existsById(1L)).willReturn(true);
        given(speakingQueueRepository.findCurrentSpeakerProjection(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.of(assigned));

        Optional<CurrentSpeakerProjection> currentSpeaker =
                speakingQueuePersistenceService.findCurrentSpeaker(1L);

        assertThat(currentSpeaker).contains(assigned);
    }

    @Test
    void findCurrentSpeaker_returnsEmptyWhenAssignedSpeakerDoesNotExist() {
        given(roomRepository.existsById(1L)).willReturn(true);
        given(speakingQueueRepository.findCurrentSpeakerProjection(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.empty());

        Optional<CurrentSpeakerProjection> currentSpeaker =
                speakingQueuePersistenceService.findCurrentSpeaker(1L);

        assertThat(currentSpeaker).isEmpty();
    }

    @Test
    void findCurrentSpeaker_rejectsMissingRoom() {
        given(roomRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() ->
                speakingQueuePersistenceService.findCurrentSpeaker(1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);

        verify(speakingQueueRepository, never())
                .findCurrentSpeakerProjection(1L, SpeakingQueueStatus.ASSIGNED);
    }

    @Test
    void expireCurrentSpeaker_completesExpiredAssignedRequestAfterLockingRoom() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        given(roomRepository.findByIdForUpdate(1L))
            .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.of(assigned));

        Optional<SpeakingQueue> expired =
                speakingQueuePersistenceService.expireCurrentSpeaker(
                        1L,
                        java.time.LocalDateTime.of(2026, 6, 12, 11, 34)
                );

        assertThat(expired).contains(assigned);
        assertThat(assigned.getStatus()).isEqualTo(SpeakingQueueStatus.COMPLETED);
        assertThat(assigned.getActiveRequest()).isNull();
        verify(speechRepository).completeSpeakingSpeeches(
                1L,
                7L,
                SpeechStatus.SPEAKING,
                SpeechStatus.COMPLETED,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 34)
        );

        InOrder order = inOrder(roomRepository, speakingQueueRepository);
        order.verify(roomRepository).findByIdForUpdate(1L);
        order.verify(speakingQueueRepository).findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        );
    }

    @Test
    void expireCurrentSpeaker_returnsEmptyWhenAssignmentIsNotExpired() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        given(roomRepository.findByIdForUpdate(1L))
            .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.of(assigned));

        Optional<SpeakingQueue> expired =
                speakingQueuePersistenceService.expireCurrentSpeaker(
                        1L,
                        java.time.LocalDateTime.of(2026, 6, 12, 11, 32)
                );

        assertThat(expired).isEmpty();
        assertThat(assigned.getStatus()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
    }

    @Test
    void expireCurrentSpeaker_returnsEmptyWhenSpeakerWasAlreadyCompleted() {
        given(roomRepository.findByIdForUpdate(1L))
            .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.empty());

        Optional<SpeakingQueue> expired =
                speakingQueuePersistenceService.expireCurrentSpeaker(
                        1L,
                        java.time.LocalDateTime.of(2026, 6, 12, 11, 34)
                );

        assertThat(expired).isEmpty();
    }

    @Test
    void recordCurrentSpeakerActivityIfMatches_updatesCurrentSpeakerActivity() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        assigned.markIdleWarningIfDue(
                ASSIGNED_AT.plusSeconds(20),
                Duration.ofSeconds(20),
                Duration.ofSeconds(40)
        );
        LocalDateTime activityAt = ASSIGNED_AT.plusSeconds(25);
        given(speakingQueueRepository.findByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.ASSIGNED)
        )).willReturn(Optional.of(assigned));
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.of(assigned));

        speakingQueuePersistenceService.recordCurrentSpeakerActivityIfMatches(
                1L,
                7L,
                activityAt
        );

        assertThat(assigned.getLastActivityAt()).isEqualTo(activityAt);
        assertThat(assigned.isIdleWarningSent()).isFalse();
        assertThat(assigned.getIdleWarnedAt()).isNull();
    }

    @Test
    void recordCurrentSpeakerActivityIfMatches_doesNothingWhenRoomCurrentSpeakerIsEmpty() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        given(speakingQueueRepository.findByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.ASSIGNED)
        )).willReturn(Optional.of(assigned));
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.empty());

        speakingQueuePersistenceService.recordCurrentSpeakerActivityIfMatches(
                1L,
                7L,
                ASSIGNED_AT.plusSeconds(10)
        );

        assertThat(assigned.getLastActivityAt()).isEqualTo(ASSIGNED_AT);
    }

    @Test
    void recordCurrentSpeakerActivityIfMatches_doesNothingWhenRoomCurrentSpeakerIsDifferentUser() {
        SpeakingQueue activeRequest = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        SpeakingQueue otherCurrentSpeaker = SpeakingQueue.waiting(
                1L,
                8L,
                16,
                SpeechStance.CON,
                LocalDateTime.of(2026, 6, 12, 11, 31)
        );
        assign(activeRequest);
        assign(otherCurrentSpeaker);
        given(speakingQueueRepository.findByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.ASSIGNED)
        )).willReturn(Optional.of(activeRequest));
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.of(otherCurrentSpeaker));

        speakingQueuePersistenceService.recordCurrentSpeakerActivityIfMatches(
                1L,
                7L,
                ASSIGNED_AT.plusSeconds(10)
        );

        assertThat(activeRequest.getLastActivityAt()).isEqualTo(ASSIGNED_AT);
    }

    @Test
    void recordCurrentSpeakerActivityIfMatches_doesNothingWhenUserIsNotCurrentSpeaker() {
        given(speakingQueueRepository.findByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.ASSIGNED)
        )).willReturn(Optional.empty());

        speakingQueuePersistenceService.recordCurrentSpeakerActivityIfMatches(
                1L,
                7L,
                ASSIGNED_AT.plusSeconds(10)
        );

        verify(roomRepository, never()).findByIdForUpdate(1L);
    }

    @Test
    void warnCurrentSpeakerIfIdle_marksWarningAfterLockingRoom() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        LocalDateTime warnedAt = ASSIGNED_AT.plusSeconds(20);
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.of(assigned));

        Optional<SpeakingQueue> warned =
                speakingQueuePersistenceService.warnCurrentSpeakerIfIdle(
                        1L,
                        warnedAt,
                        Duration.ofSeconds(20),
                        Duration.ofSeconds(40)
                );

        assertThat(warned).contains(assigned);
        assertThat(assigned.isIdleWarningSent()).isTrue();
        assertThat(assigned.getIdleWarnedAt()).isEqualTo(warnedAt);
    }

    @Test
    void warnCurrentSpeakerIfIdle_returnsEmptyWhenWarningIsNotDue() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.of(assigned));

        Optional<SpeakingQueue> warned =
                speakingQueuePersistenceService.warnCurrentSpeakerIfIdle(
                        1L,
                        ASSIGNED_AT.plusSeconds(19),
                        Duration.ofSeconds(20),
                        Duration.ofSeconds(40)
                );

        assertThat(warned).isEmpty();
        assertThat(assigned.isIdleWarningSent()).isFalse();
    }

    @Test
    void warnCurrentSpeakerIfIdle_returnsEmptyWhenCurrentSpeakerDoesNotExist() {
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.empty());

        Optional<SpeakingQueue> warned =
                speakingQueuePersistenceService.warnCurrentSpeakerIfIdle(
                        1L,
                        ASSIGNED_AT.plusSeconds(20),
                        Duration.ofSeconds(20),
                        Duration.ofSeconds(40)
                );

        assertThat(warned).isEmpty();
    }

    @Test
    void completeCurrentSpeakerIfIdleTimedOut_completesAfterWarningDelay() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        assigned.markIdleWarningIfDue(
                ASSIGNED_AT.plusSeconds(20),
                Duration.ofSeconds(20),
                Duration.ofSeconds(40)
        );
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.of(assigned));

        Optional<SpeakingQueue> completed =
                speakingQueuePersistenceService.completeCurrentSpeakerIfIdleTimedOut(
                        1L,
                        ASSIGNED_AT.plusSeconds(40),
                        Duration.ofSeconds(20)
                );

        assertThat(completed).contains(assigned);
        assertThat(assigned.getStatus()).isEqualTo(SpeakingQueueStatus.COMPLETED);
        assertThat(assigned.getActiveRequest()).isNull();
        verify(speechRepository).completeSpeakingSpeeches(
                1L,
                7L,
                SpeechStatus.SPEAKING,
                SpeechStatus.COMPLETED,
                ASSIGNED_AT.plusSeconds(40)
        );
    }

    @Test
    void completeCurrentSpeakerIfIdleTimedOut_returnsEmptyWhenCurrentSpeakerDoesNotExist() {
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.empty());

        Optional<SpeakingQueue> completed =
                speakingQueuePersistenceService.completeCurrentSpeakerIfIdleTimedOut(
                        1L,
                        ASSIGNED_AT.plusSeconds(40),
                        Duration.ofSeconds(20)
                );

        assertThat(completed).isEmpty();
    }

    @Test
    void completeCurrentSpeakerIfIdleTimedOut_returnsEmptyWhenIdleTimeoutIsNotDue() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        assigned.markIdleWarningIfDue(
                ASSIGNED_AT.plusSeconds(20),
                Duration.ofSeconds(20),
                Duration.ofSeconds(40)
        );
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(openRoom(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.of(assigned));

        Optional<SpeakingQueue> completed =
                speakingQueuePersistenceService.completeCurrentSpeakerIfIdleTimedOut(
                        1L,
                        ASSIGNED_AT.plusSeconds(39),
                        Duration.ofSeconds(20)
                );

        assertThat(completed).isEmpty();
        assertThat(assigned.getStatus()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
    }

    private void givenJoined(Long roomId, Long userId) {
        given(roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                roomId,
                userId,
                RoomParticipantStatus.JOINED
        )).willReturn(true);
    }

    private void givenEligibility(
            Long roomId,
            Long userId,
            SpeakingRequestEligibilityProjection eligibility
    ) {
        given(speakingQueueRepository.findSpeakingRequestEligibility(
                eq(roomId),
                eq(userId),
                any(LocalDateTime.class)
        )).willReturn(eligibility);
    }

    private SpeakingRequestEligibilityProjection eligibility(
            Integer roomExists,
            Integer roomActive,
            Integer joinedParticipant,
            Integer activeRequestExists,
            Integer restricted
    ) {
        return new SpeakingRequestEligibilityProjection() {
            @Override
            public Integer getRoomExists() {
                return roomExists;
            }

            @Override
            public Integer getRoomActive() {
                return roomActive;
            }

            @Override
            public Integer getJoinedParticipant() {
                return joinedParticipant;
            }

            @Override
            public Integer getActiveRequestExists() {
                return activeRequestExists;
            }

            @Override
            public Integer getRestricted() {
                return restricted;
            }
        };
    }

    private void assign(SpeakingQueue speakingQueue) {
        speakingQueue.assign(
                java.time.LocalDateTime.of(2026, 6, 12, 11, 31),
                java.time.LocalDateTime.of(2026, 6, 12, 11, 33)
        );
    }

    private SpeakingQueue completedAssignment(
            Long userId,
            int queueOrder,
            SpeechStance stance,
            int hour,
            int minute
    ) {
        LocalDateTime assignedAt = LocalDateTime.of(2026, 6, 12, hour, minute);
        SpeakingQueue speakingQueue = SpeakingQueue.waiting(
                1L,
                userId,
                queueOrder,
                stance,
                assignedAt.minusMinutes(1)
        );
        speakingQueue.assign(assignedAt, assignedAt.plusMinutes(3));
        speakingQueue.complete();
        return speakingQueue;
    }

    private CurrentSpeakerProjection currentSpeakerProjection() {
        return new CurrentSpeakerProjection() {
            @Override
            public Long getUserId() {
                return 7L;
            }

            @Override
            public String getNickname() {
                return "logic_hunter";
            }

            @Override
            public SpeechStance getStance() {
                return SpeechStance.PRO;
            }

            @Override
            public Integer getQueueOrder() {
                return 15;
            }

            @Override
            public java.time.LocalDateTime getAssignedAt() {
                return ASSIGNED_AT;
            }

            @Override
            public java.time.LocalDateTime getExpiresAt() {
                return EXPIRES_AT;
            }
        };
    }
}
