package com.sisibibi.api.domain.speech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
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
    private RoomRepository roomRepository;

    @InjectMocks
    private SpeakingQueuePersistenceService speakingQueuePersistenceService;

    @Test
    void createWaitingRequest_persistsRequestAndAssignsOrderFromId() {
        given(speakingQueueRepository.existsByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        )).willReturn(false);
        given(speakingQueueRepository.saveAndFlush(any(SpeakingQueue.class)))
                .willAnswer(invocation -> {
                    SpeakingQueue saved = invocation.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", 15L);
                    return saved;
                });

        SpeakingQueue saved =
                speakingQueuePersistenceService.createWaitingRequest(1L, 7L);

        assertThat(saved.getQueueOrder()).isEqualTo(15);
        assertThat(saved.getStatus()).isEqualTo(SpeakingQueueStatus.WAITING);
    }

    @Test
    void createWaitingRequest_rejectsExistingActiveRequest() {
        given(speakingQueueRepository.existsByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        )).willReturn(true);

        assertThatThrownBy(() ->
                speakingQueuePersistenceService.createWaitingRequest(1L, 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS);

        verify(speakingQueueRepository, never()).saveAndFlush(any(SpeakingQueue.class));
    }

    @Test
    void cancelWaitingRequest_cancelsWaitingRequestAfterLockingRoom() {
        SpeakingQueue waiting = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(Room.open(1L, "토론방")));
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
                .willReturn(Optional.of(Room.open(1L, "토론방")));
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
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(Room.open(1L, "토론방")));
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
    void assignNextSpeaker_assignsFirstWaitingRequestWhenCurrentSpeakerDoesNotExist() {
        SpeakingQueue firstWaiting = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(Room.open(1L, "토론방")));
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

        Optional<SpeakingQueue> assigned =
                speakingQueuePersistenceService.assignNextSpeaker(
                        1L,
                        ASSIGNED_AT,
                        EXPIRES_AT
                );

        assertThat(assigned).contains(firstWaiting);
        assertThat(firstWaiting.getStatus()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(firstWaiting.getAssignedAt()).isEqualTo(ASSIGNED_AT);
        assertThat(firstWaiting.getExpiresAt()).isEqualTo(EXPIRES_AT);

        InOrder order = inOrder(roomRepository, speakingQueueRepository);
        order.verify(roomRepository).findByIdForUpdate(1L);
        order.verify(speakingQueueRepository)
                .existsByRoomIdAndStatus(1L, SpeakingQueueStatus.ASSIGNED);
        order.verify(speakingQueueRepository)
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING
                );
    }

    @Test
    void assignNextSpeaker_doesNotAssignWhenCurrentSpeakerAlreadyExists() {
        SpeakingQueue waiting = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(Room.open(1L, "토론방")));
        given(speakingQueueRepository.existsByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(true);

        Optional<SpeakingQueue> assigned =
                speakingQueuePersistenceService.assignNextSpeaker(
                        1L,
                        ASSIGNED_AT,
                        EXPIRES_AT
                );

        assertThat(assigned).isEmpty();
        assertThat(waiting.getStatus()).isEqualTo(SpeakingQueueStatus.WAITING);
        verify(speakingQueueRepository, never())
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING
                );
    }

    @Test
    void assignNextSpeaker_returnsEmptyWhenWaitingQueueIsEmpty() {
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(Room.open(1L, "토론방")));
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

        Optional<SpeakingQueue> assigned =
                speakingQueuePersistenceService.assignNextSpeaker(
                        1L,
                        ASSIGNED_AT,
                        EXPIRES_AT
                );

        assertThat(assigned).isEmpty();
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

        verify(speakingQueueRepository, never())
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
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(Room.open(1L, "토론방")));
        given(speakingQueueRepository.findByRoomIdAndStatus(
                1L,
                SpeakingQueueStatus.ASSIGNED
        )).willReturn(Optional.of(assigned));

        SpeakingQueue completed =
                speakingQueuePersistenceService.completeCurrentSpeaker(1L, 7L);

        assertThat(completed).isSameAs(assigned);
        assertThat(completed.getStatus()).isEqualTo(SpeakingQueueStatus.COMPLETED);
        assertThat(completed.getActiveRequest()).isNull();

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
                .willReturn(Optional.of(Room.open(1L, "토론방")));
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
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(Room.open(1L, "토론방")));
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
    void expireCurrentSpeaker_completesExpiredAssignedRequestAfterLockingRoom() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(Room.open(1L, "토론방")));
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
                java.time.LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        given(roomRepository.findByIdForUpdate(1L))
                .willReturn(Optional.of(Room.open(1L, "토론방")));
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
                .willReturn(Optional.of(Room.open(1L, "토론방")));
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

    private void assign(SpeakingQueue speakingQueue) {
        speakingQueue.assign(
                java.time.LocalDateTime.of(2026, 6, 12, 11, 31),
                java.time.LocalDateTime.of(2026, 6, 12, 11, 33)
        );
    }
}
