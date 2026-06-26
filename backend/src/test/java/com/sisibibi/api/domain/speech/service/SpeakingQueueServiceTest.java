package com.sisibibi.api.domain.speech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.speech.dto.event.StageChangedEvent;
import com.sisibibi.api.domain.speech.dto.event.StageEventType;
import com.sisibibi.api.domain.speech.dto.event.StageTurnEndReason;
import com.sisibibi.api.domain.speech.config.SpeakingQueueProperties;
import com.sisibibi.api.domain.speech.dto.response.StageCurrentSpeakerRes;
import com.sisibibi.api.domain.speech.dto.response.StageQueueRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestStatusRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.RedisSpeakingQueueRepository;
import com.sisibibi.api.domain.speech.repository.projection.CurrentSpeakerProjection;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SpeakingQueueServiceTest {

    @Mock
    private RedisSpeakingQueueRepository redisSpeakingQueueRepository;

    @Mock
    private SpeakingQueuePersistenceService speakingQueuePersistenceService;

    @Mock
    private SpeakingQueueProperties speakingQueueProperties;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AiCounterIssueService aiCounterIssueService;

    @InjectMocks
    private SpeakingQueueService speakingQueueService;

    @Test
    void requestSpeakingTurn_attemptsImmediateAssignmentAndReturnsAssignedRequest() {
        SpeakingQueue saved = persistedWaitingRequest(1L, 7L, 15);
        SpeakingQueue assigned = assignedRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.createWaitingRequest(1L, 7L, SpeechStance.PRO))
                .willReturn(saved);
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofMinutes(2));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(assignmentResult(assigned));

        StageRequestRes response =
                speakingQueueService.requestSpeakingTurn(1L, 7L, SpeechStance.PRO);

        assertThat(response.roomId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.stance()).isEqualTo(SpeechStance.PRO);
        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(response.queueOrder()).isEqualTo(15);
        verify(redisSpeakingQueueRepository).upsert(1L, 7L, 15);
        verify(redisSpeakingQueueRepository).assign(1L, 7L);
        verify(speakingQueuePersistenceService).assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(aiCounterIssueService, never()).suggestIfNeeded(1L);
        ArgumentCaptor<StageChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(StageChangedEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .extracting(StageChangedEvent::type)
                .containsExactly(
                        StageEventType.SPEAKING_REQUESTED,
                        StageEventType.SPEAKER_ASSIGNED
                );
    }

    @Test
    void requestSpeakingTurn_doesNotWriteRedisWhenDbPersistenceFails() {
        given(speakingQueuePersistenceService.createWaitingRequest(1L, 7L, SpeechStance.PRO))
                .willThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() ->
                speakingQueueService.requestSpeakingTurn(1L, 7L, SpeechStance.PRO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        verify(redisSpeakingQueueRepository, never())
                .upsert(org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyInt());
        verify(speakingQueuePersistenceService, never()).assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void requestSpeakingTurn_stillAttemptsImmediateAssignmentWhenRedisSynchronizationFails() {
        SpeakingQueue saved = persistedWaitingRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.createWaitingRequest(1L, 7L, SpeechStance.PRO))
                .willReturn(saved);
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofMinutes(2));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(SpeakingQueueAssignmentResult.empty());
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisSpeakingQueueRepository)
                .upsert(1L, 7L, 15);
        given(speakingQueuePersistenceService.findWaitingRequestsForRedisProjection(1L))
                .willReturn(List.of(saved));
        given(speakingQueuePersistenceService.findCurrentSpeakerForRedisProjection(1L))
                .willReturn(Optional.empty());
        given(redisSpeakingQueueRepository.replaceRoomProjectionIfVersionMatches(
                1L,
                List.of(saved),
                Optional.empty(),
                0L
        )).willReturn(true);

        StageRequestRes response =
                speakingQueueService.requestSpeakingTurn(1L, 7L, SpeechStance.PRO);

        assertThat(response.queueOrder()).isEqualTo(15);
        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        verify(speakingQueuePersistenceService).createWaitingRequest(1L, 7L, SpeechStance.PRO);
        verify(speakingQueuePersistenceService).assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(redisSpeakingQueueRepository)
                .replaceRoomProjectionIfVersionMatches(
                        1L,
                        List.of(saved),
                        Optional.empty(),
                        0L
                );
        ArgumentCaptor<StageChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(StageChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type())
                .isEqualTo(StageEventType.SPEAKING_REQUESTED);
    }

    @Test
    void requestSpeakingTurn_retriesRedisProjectionRebuildWhenRebuildTemporarilyFails() {
        SpeakingQueue saved = persistedWaitingRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.createWaitingRequest(1L, 7L, SpeechStance.PRO))
                .willReturn(saved);
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofMinutes(2));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(SpeakingQueueAssignmentResult.empty());
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisSpeakingQueueRepository)
                .upsert(1L, 7L, 15);
        given(speakingQueuePersistenceService.findWaitingRequestsForRedisProjection(1L))
                .willReturn(List.of(saved));
        given(speakingQueuePersistenceService.findCurrentSpeakerForRedisProjection(1L))
                .willReturn(Optional.empty());
        given(redisSpeakingQueueRepository.replaceRoomProjectionIfVersionMatches(
                1L,
                List.of(saved),
                Optional.empty(),
                0L
        ))
                .willThrow(new IllegalStateException("temporary rebuild failure"))
                .willThrow(new IllegalStateException("temporary rebuild failure"))
                .willReturn(true);

        StageRequestRes response =
                speakingQueueService.requestSpeakingTurn(1L, 7L, SpeechStance.PRO);

        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        verify(redisSpeakingQueueRepository, times(3))
                .replaceRoomProjectionIfVersionMatches(
                        1L,
                        List.of(saved),
                        Optional.empty(),
                        0L
                );
        verify(speakingQueuePersistenceService, times(3))
                .findWaitingRequestsForRedisProjection(1L);
        verify(speakingQueuePersistenceService, times(3))
                .findCurrentSpeakerForRedisProjection(1L);
    }

    @Test
    void requestSpeakingTurn_retriesRedisProjectionRebuildWhenProjectionSourceLoadFails() {
        SpeakingQueue saved = persistedWaitingRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.createWaitingRequest(1L, 7L, SpeechStance.PRO))
                .willReturn(saved);
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofMinutes(2));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(SpeakingQueueAssignmentResult.empty());
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisSpeakingQueueRepository)
                .upsert(1L, 7L, 15);
        given(speakingQueuePersistenceService.findWaitingRequestsForRedisProjection(1L))
                .willThrow(new IllegalStateException("temporary source load failure"))
                .willReturn(List.of(saved));
        given(speakingQueuePersistenceService.findCurrentSpeakerForRedisProjection(1L))
                .willReturn(Optional.empty());
        given(redisSpeakingQueueRepository.replaceRoomProjectionIfVersionMatches(
                1L,
                List.of(saved),
                Optional.empty(),
                0L
        )).willReturn(true);

        StageRequestRes response =
                speakingQueueService.requestSpeakingTurn(1L, 7L, SpeechStance.PRO);

        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        verify(speakingQueuePersistenceService, times(2))
                .findWaitingRequestsForRedisProjection(1L);
        verify(speakingQueuePersistenceService)
                .findCurrentSpeakerForRedisProjection(1L);
        verify(redisSpeakingQueueRepository)
                .replaceRoomProjectionIfVersionMatches(
                        1L,
                        List.of(saved),
                        Optional.empty(),
                        0L
                );
    }

    @Test
    void requestSpeakingTurn_reloadsDbSnapshotWhenProjectionVersionChangesBeforeRebuild() {
        SpeakingQueue saved = persistedWaitingRequest(1L, 7L, 15);
        SpeakingQueue laterWaiting = persistedWaitingRequest(1L, 8L, 16);
        given(speakingQueuePersistenceService.createWaitingRequest(1L, 7L, SpeechStance.PRO))
                .willReturn(saved);
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofMinutes(2));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(SpeakingQueueAssignmentResult.empty());
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisSpeakingQueueRepository)
                .upsert(1L, 7L, 15);
        given(redisSpeakingQueueRepository.currentProjectionVersion(1L))
                .willReturn(0L, 1L);
        given(speakingQueuePersistenceService.findWaitingRequestsForRedisProjection(1L))
                .willReturn(List.of(saved), List.of(saved, laterWaiting));
        given(speakingQueuePersistenceService.findCurrentSpeakerForRedisProjection(1L))
                .willReturn(Optional.empty());
        given(redisSpeakingQueueRepository.replaceRoomProjectionIfVersionMatches(
                1L,
                List.of(saved),
                Optional.empty(),
                0L
        )).willReturn(false);
        given(redisSpeakingQueueRepository.replaceRoomProjectionIfVersionMatches(
                1L,
                List.of(saved, laterWaiting),
                Optional.empty(),
                1L
        )).willReturn(true);

        StageRequestRes response =
                speakingQueueService.requestSpeakingTurn(1L, 7L, SpeechStance.PRO);

        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        verify(redisSpeakingQueueRepository)
                .replaceRoomProjectionIfVersionMatches(
                        1L,
                        List.of(saved),
                        Optional.empty(),
                        0L
                );
        verify(redisSpeakingQueueRepository)
                .replaceRoomProjectionIfVersionMatches(
                        1L,
                        List.of(saved, laterWaiting),
                        Optional.empty(),
                        1L
                );
        verify(speakingQueuePersistenceService, times(2))
                .findWaitingRequestsForRedisProjection(1L);
        verify(speakingQueuePersistenceService, times(2))
                .findCurrentSpeakerForRedisProjection(1L);
    }

    @Test
    void requestSpeakingTurn_stopsRedisProjectionRebuildAfterProjectionSourceLoadRetriesFail() {
        SpeakingQueue saved = persistedWaitingRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.createWaitingRequest(1L, 7L, SpeechStance.PRO))
                .willReturn(saved);
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofMinutes(2));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(SpeakingQueueAssignmentResult.empty());
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisSpeakingQueueRepository)
                .upsert(1L, 7L, 15);
        given(speakingQueuePersistenceService.findWaitingRequestsForRedisProjection(1L))
                .willThrow(new IllegalStateException("database unavailable"));

        StageRequestRes response =
                speakingQueueService.requestSpeakingTurn(1L, 7L, SpeechStance.PRO);

        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        verify(speakingQueuePersistenceService, times(3))
                .findWaitingRequestsForRedisProjection(1L);
        verify(speakingQueuePersistenceService, never())
                .findCurrentSpeakerForRedisProjection(1L);
        verify(redisSpeakingQueueRepository, never())
                .replaceRoomProjectionIfVersionMatches(any(), any(), any(), anyLong());
    }

    @Test
    void requestSpeakingTurn_keepsCreatedRequestWhenImmediateAssignmentFails() {
        SpeakingQueue saved = persistedWaitingRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.createWaitingRequest(1L, 7L, SpeechStance.PRO))
                .willReturn(saved);
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofMinutes(2));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willThrow(new IllegalStateException("assignment failed"));

        StageRequestRes response =
                speakingQueueService.requestSpeakingTurn(1L, 7L, SpeechStance.PRO);

        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(response.queueOrder()).isEqualTo(15);
        verify(redisSpeakingQueueRepository).upsert(1L, 7L, 15);
        ArgumentCaptor<StageChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(StageChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type())
                .isEqualTo(StageEventType.SPEAKING_REQUESTED);
    }

    @Test
    void assignNextSpeaker_rebuildsRedisProjectionWhenAssignedSynchronizationFails() {
        SpeakingQueue assigned = assignedRequest(1L, 7L, 15);
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofMinutes(2));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(assignmentResult(assigned));
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisSpeakingQueueRepository)
                .assign(1L, 7L);
        given(speakingQueuePersistenceService.findWaitingRequestsForRedisProjection(1L))
                .willReturn(List.of());
        given(speakingQueuePersistenceService.findCurrentSpeakerForRedisProjection(1L))
                .willReturn(Optional.of(assigned));
        given(redisSpeakingQueueRepository.replaceRoomProjectionIfVersionMatches(
                1L,
                List.of(),
                Optional.of(assigned),
                0L
        )).willReturn(true);

        Optional<SpeakingQueue> response = speakingQueueService.assignNextSpeaker(1L);

        assertThat(response).contains(assigned);
        verify(redisSpeakingQueueRepository).assign(1L, 7L);
        verify(redisSpeakingQueueRepository)
                .replaceRoomProjectionIfVersionMatches(
                        1L,
                        List.of(),
                        Optional.of(assigned),
                        0L
                );
        ArgumentCaptor<StageChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(StageChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type())
                .isEqualTo(StageEventType.SPEAKER_ASSIGNED);
    }

    @Test
    void assignNextSpeaker_removesCanceledRequestsWhenWaitingParticipantLeftRoom() {
        SpeakingQueue canceled = persistedWaitingRequest(1L, 7L, 15);
        canceled.cancel(LocalDateTime.of(2026, 6, 12, 11, 31));
        SpeakingQueue assigned = assignedRequest(1L, 8L, 16);
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofMinutes(2));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(SpeakingQueueAssignmentResult.of(
                Optional.of(assigned),
                List.of(canceled)
        ));

        Optional<SpeakingQueue> response = speakingQueueService.assignNextSpeaker(1L);

        assertThat(response).contains(assigned);
        verify(redisSpeakingQueueRepository).remove(1L, 7L);
        verify(redisSpeakingQueueRepository).assign(1L, 8L);
        ArgumentCaptor<StageChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(StageChangedEvent.class);
        verify(eventPublisher, times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .extracting(StageChangedEvent::type)
                .containsExactly(
                        StageEventType.SPEAKING_CANCELED,
                        StageEventType.SPEAKER_ASSIGNED
                );
    }

    @Test
    void assignNextSpeaker_appliesConfiguredTurnDuration() {
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofSeconds(90));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(SpeakingQueueAssignmentResult.empty());

        speakingQueueService.assignNextSpeaker(1L);

        ArgumentCaptor<LocalDateTime> assignedAtCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> expiresAtCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        verify(speakingQueuePersistenceService).assignNextSpeaker(
                eq(1L),
                assignedAtCaptor.capture(),
                expiresAtCaptor.capture()
        );
        assertThat(Duration.between(
                assignedAtCaptor.getValue(),
                expiresAtCaptor.getValue()
        )).isEqualTo(Duration.ofSeconds(90));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void cancelSpeakingRequest_cancelsDurableRequestAndRemovesRedisProjection() {
        SpeakingQueue canceled = persistedWaitingRequest(1L, 7L, 15);
        canceled.cancel(LocalDateTime.of(2026, 6, 12, 11, 35));
        given(speakingQueuePersistenceService.cancelWaitingRequest(1L, 7L))
                .willReturn(canceled);
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofMinutes(2));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(SpeakingQueueAssignmentResult.empty());

        speakingQueueService.cancelSpeakingRequest(1L, 7L);

        verify(speakingQueuePersistenceService).cancelWaitingRequest(1L, 7L);
        verify(redisSpeakingQueueRepository).remove(1L, 7L);
        verify(speakingQueuePersistenceService).assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(aiCounterIssueService, never()).suggestIfNeeded(1L);
        ArgumentCaptor<StageChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(StageChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type())
                .isEqualTo(StageEventType.SPEAKING_CANCELED);
    }

    @Test
    void cancelSpeakingRequest_keepsCanceledDbStateWhenRedisRemovalFails() {
        SpeakingQueue canceled = persistedWaitingRequest(1L, 7L, 15);
        canceled.cancel(LocalDateTime.of(2026, 6, 12, 11, 35));
        given(speakingQueuePersistenceService.cancelWaitingRequest(1L, 7L))
                .willReturn(canceled);
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofMinutes(2));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(SpeakingQueueAssignmentResult.empty());
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisSpeakingQueueRepository)
                .remove(1L, 7L);
        given(speakingQueuePersistenceService.findWaitingRequestsForRedisProjection(1L))
                .willReturn(List.of());
        given(speakingQueuePersistenceService.findCurrentSpeakerForRedisProjection(1L))
                .willReturn(Optional.empty());
        given(redisSpeakingQueueRepository.replaceRoomProjectionIfVersionMatches(
                1L,
                List.of(),
                Optional.empty(),
                0L
        )).willReturn(true);

        speakingQueueService.cancelSpeakingRequest(1L, 7L);

        verify(speakingQueuePersistenceService).cancelWaitingRequest(1L, 7L);
        verify(redisSpeakingQueueRepository).remove(1L, 7L);
        verify(redisSpeakingQueueRepository)
                .replaceRoomProjectionIfVersionMatches(
                        1L,
                        List.of(),
                        Optional.empty(),
                        0L
                );
        verify(speakingQueuePersistenceService).assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(aiCounterIssueService, never()).suggestIfNeeded(1L);
        ArgumentCaptor<StageChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(StageChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type())
                .isEqualTo(StageEventType.SPEAKING_CANCELED);
    }

    @Test
    void getMySpeakingRequestStatus_returnsEmptyResponseWhenActiveRequestDoesNotExist() {
        given(speakingQueuePersistenceService.findMyActiveRequest(1L, 7L))
                .willReturn(Optional.empty());

        StageRequestStatusRes response =
                speakingQueueService.getMySpeakingRequestStatus(1L, 7L);

        assertThat(response.hasRequest()).isFalse();
        assertThat(response.status()).isNull();
        assertThat(response.cancelable()).isFalse();
    }

    @Test
    void getMySpeakingRequestStatus_returnsWaitingRequestStatus() {
        SpeakingQueue waiting = persistedWaitingRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.findMyActiveRequest(1L, 7L))
                .willReturn(Optional.of(waiting));
        given(redisSpeakingQueueRepository.rank(1L, 7L))
                .willReturn(Optional.of(3));

        StageRequestStatusRes response =
                speakingQueueService.getMySpeakingRequestStatus(1L, 7L);

        assertThat(response.hasRequest()).isTrue();
        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(response.roomId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.stance()).isEqualTo(SpeechStance.PRO);
        assertThat(response.queueOrder()).isEqualTo(15);
        assertThat(response.currentRank()).isEqualTo(3);
        assertThat(response.cancelable()).isTrue();
        assertThat(response.requestedAt())
                .isEqualTo(LocalDateTime.of(2026, 6, 12, 11, 30));
        assertThat(response.assignedAt()).isNull();
        assertThat(response.expiresAt()).isNull();
    }

    @Test
    void getMySpeakingRequestStatus_returnsDbRankWhenWaitingRequestIsMissingInRedis() {
        SpeakingQueue waiting = persistedWaitingRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.findMyActiveRequest(1L, 7L))
                .willReturn(Optional.of(waiting));
        given(redisSpeakingQueueRepository.rank(1L, 7L))
                .willReturn(Optional.empty());
        given(speakingQueuePersistenceService.countWaitingRequestsBefore(1L, 15))
                .willReturn(4L);

        StageRequestStatusRes response =
                speakingQueueService.getMySpeakingRequestStatus(1L, 7L);

        assertThat(response.hasRequest()).isTrue();
        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(response.currentRank()).isEqualTo(5);
    }

    @Test
    void getMySpeakingRequestStatus_returnsDbRankWhenRedisRankReadFails() {
        SpeakingQueue waiting = persistedWaitingRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.findMyActiveRequest(1L, 7L))
                .willReturn(Optional.of(waiting));
        given(redisSpeakingQueueRepository.rank(1L, 7L))
                .willThrow(new IllegalStateException("redis unavailable"));
        given(speakingQueuePersistenceService.countWaitingRequestsBefore(1L, 15))
                .willReturn(2L);

        StageRequestStatusRes response =
                speakingQueueService.getMySpeakingRequestStatus(1L, 7L);

        assertThat(response.hasRequest()).isTrue();
        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(response.currentRank()).isEqualTo(3);
    }

    @Test
    void getMySpeakingRequestStatus_returnsAssignedRequestStatus() {
        SpeakingQueue assigned = assignedRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.findMyActiveRequest(1L, 7L))
                .willReturn(Optional.of(assigned));

        StageRequestStatusRes response =
                speakingQueueService.getMySpeakingRequestStatus(1L, 7L);

        assertThat(response.hasRequest()).isTrue();
        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(response.queueOrder()).isEqualTo(15);
        assertThat(response.currentRank()).isNull();
        assertThat(response.cancelable()).isFalse();
        assertThat(response.assignedAt())
                .isEqualTo(LocalDateTime.of(2026, 6, 12, 11, 31));
        assertThat(response.expiresAt())
                .isEqualTo(LocalDateTime.of(2026, 6, 12, 11, 33));
        verify(redisSpeakingQueueRepository, never()).rank(1L, 7L);
    }

    @Test
    void completeSpeakingTurn_completesDurableRequestAndRemovesCurrentSpeaker() {
        SpeakingQueue completed = completedRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.completeCurrentSpeaker(1L, 7L))
                .willReturn(completed);
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofMinutes(2));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(SpeakingQueueAssignmentResult.empty());

        speakingQueueService.completeSpeakingTurn(1L, 7L);

        verify(speakingQueuePersistenceService).completeCurrentSpeaker(1L, 7L);
        verify(redisSpeakingQueueRepository).removeCurrentSpeaker(1L, 7L);
        verify(speakingQueuePersistenceService).assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(aiCounterIssueService).suggestIfNeeded(1L);
        ArgumentCaptor<StageChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(StageChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type())
                .isEqualTo(StageEventType.SPEAKER_COMPLETED);
        assertThat(eventCaptor.getValue().payload().endReason())
                .isEqualTo(StageTurnEndReason.COMPLETED);
    }

    @Test
    void completeSpeakingTurn_suggestsAiCounterIssueBeforeAssigningNextSpeaker() {
        SpeakingQueue completed = completedRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.completeCurrentSpeaker(1L, 7L))
                .willReturn(completed);
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofMinutes(2));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(SpeakingQueueAssignmentResult.empty());

        speakingQueueService.completeSpeakingTurn(1L, 7L);

        InOrder order = inOrder(speakingQueuePersistenceService, aiCounterIssueService);
        order.verify(speakingQueuePersistenceService).completeCurrentSpeaker(1L, 7L);
        order.verify(aiCounterIssueService).suggestIfNeeded(1L);
        order.verify(speakingQueuePersistenceService).assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
    }

    @Test
    void completeSpeakingTurn_keepsCompletedDbStateWhenRedisRemovalFails() {
        SpeakingQueue completed = completedRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.completeCurrentSpeaker(1L, 7L))
                .willReturn(completed);
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofMinutes(2));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(SpeakingQueueAssignmentResult.empty());
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisSpeakingQueueRepository)
                .removeCurrentSpeaker(1L, 7L);
        given(speakingQueuePersistenceService.findWaitingRequestsForRedisProjection(1L))
                .willReturn(List.of());
        given(speakingQueuePersistenceService.findCurrentSpeakerForRedisProjection(1L))
                .willReturn(Optional.empty());
        given(redisSpeakingQueueRepository.replaceRoomProjectionIfVersionMatches(
                1L,
                List.of(),
                Optional.empty(),
                0L
        )).willReturn(true);

        speakingQueueService.completeSpeakingTurn(1L, 7L);

        verify(speakingQueuePersistenceService).completeCurrentSpeaker(1L, 7L);
        verify(redisSpeakingQueueRepository).removeCurrentSpeaker(1L, 7L);
        verify(redisSpeakingQueueRepository)
                .replaceRoomProjectionIfVersionMatches(
                        1L,
                        List.of(),
                        Optional.empty(),
                        0L
                );
        verify(speakingQueuePersistenceService).assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(aiCounterIssueService).suggestIfNeeded(1L);
        ArgumentCaptor<StageChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(StageChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type())
                .isEqualTo(StageEventType.SPEAKER_COMPLETED);
        assertThat(eventCaptor.getValue().payload().endReason())
                .isEqualTo(StageTurnEndReason.COMPLETED);
    }

    @Test
    void completeCurrentSpeakerWhenParticipantLeft_completesTurnWithLeftRoomReason() {
        SpeakingQueue completed = completedRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.completeCurrentSpeakerIfMatches(1L, 7L))
                .willReturn(Optional.of(completed));
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofMinutes(2));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(SpeakingQueueAssignmentResult.empty());

        speakingQueueService.completeCurrentSpeakerWhenParticipantLeft(1L, 7L);

        verify(speakingQueuePersistenceService).completeCurrentSpeakerIfMatches(1L, 7L);
        verify(redisSpeakingQueueRepository).removeCurrentSpeaker(1L, 7L);
        verify(speakingQueuePersistenceService).assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(aiCounterIssueService).suggestIfNeeded(1L);
        ArgumentCaptor<StageChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(StageChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type())
                .isEqualTo(StageEventType.SPEAKER_COMPLETED);
        assertThat(eventCaptor.getValue().payload().endReason())
                .isEqualTo(StageTurnEndReason.LEFT_ROOM);
    }

    @Test
    void completeCurrentSpeakerWhenParticipantLeft_doesNothingWhenLeavingUserIsNotCurrentSpeaker() {
        given(speakingQueuePersistenceService.completeCurrentSpeakerIfMatches(1L, 8L))
                .willReturn(Optional.empty());

        speakingQueueService.completeCurrentSpeakerWhenParticipantLeft(1L, 8L);

        verify(speakingQueuePersistenceService).completeCurrentSpeakerIfMatches(1L, 8L);
        verify(redisSpeakingQueueRepository, never()).removeCurrentSpeaker(anyLong(), anyLong());
        verify(speakingQueuePersistenceService, never()).assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void closeSpeakingQueuesWhenRoomClosed_cleansRedisWithoutAssigningNextSpeaker() {
        LocalDateTime closedAt = LocalDateTime.of(2026, 6, 24, 12, 0);
        SpeakingQueue canceled = persistedWaitingRequest(1L, 7L, 15);
        canceled.cancel(closedAt);
        SpeakingQueue completed = completedRequest(1L, 8L, 16);
        given(speakingQueuePersistenceService.closeActiveRequestsByRoomId(1L, closedAt))
                .willReturn(SpeakingQueueRoomCloseResult.of(
                        List.of(canceled),
                        List.of(completed)
                ));

        speakingQueueService.closeSpeakingQueuesWhenRoomClosed(1L, closedAt);

        verify(speakingQueuePersistenceService).closeActiveRequestsByRoomId(1L, closedAt);
        verify(redisSpeakingQueueRepository).remove(1L, 7L);
        verify(redisSpeakingQueueRepository).removeCurrentSpeaker(1L, 8L);
        verify(speakingQueuePersistenceService, never()).assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void expireCurrentSpeaker_removesExpiredSpeakerFromRedis() {
        SpeakingQueue completed = completedRequest(1L, 7L, 15);
        LocalDateTime now = LocalDateTime.of(2026, 6, 12, 11, 34);
        given(speakingQueuePersistenceService.expireCurrentSpeaker(1L, now))
                .willReturn(Optional.of(completed));
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofMinutes(2));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(SpeakingQueueAssignmentResult.empty());

        Optional<SpeakingQueue> expired =
                speakingQueueService.expireCurrentSpeaker(1L, now);

        assertThat(expired).contains(completed);
        verify(redisSpeakingQueueRepository).removeCurrentSpeaker(1L, 7L);
        verify(speakingQueuePersistenceService).assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(aiCounterIssueService).suggestIfNeeded(1L);
        ArgumentCaptor<StageChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(StageChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type())
                .isEqualTo(StageEventType.SPEAKER_EXPIRED);
        assertThat(eventCaptor.getValue().payload().status())
                .isEqualTo(SpeakingQueueStatus.COMPLETED);
        assertThat(eventCaptor.getValue().payload().endReason())
                .isEqualTo(StageTurnEndReason.EXPIRED);
    }

    @Test
    void expireCurrentSpeaker_doesNotTouchRedisWhenSpeakerIsNotExpired() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 12, 11, 32);
        given(speakingQueuePersistenceService.expireCurrentSpeaker(1L, now))
                .willReturn(Optional.empty());

        Optional<SpeakingQueue> expired =
                speakingQueueService.expireCurrentSpeaker(1L, now);

        assertThat(expired).isEmpty();
        verify(redisSpeakingQueueRepository, never())
                .removeCurrentSpeaker(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong()
                );
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void warnIdleCurrentSpeaker_publishesIdleWarningEvent() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 12, 11, 31, 20);
        SpeakingQueue warned = assignedRequest(1L, 7L, 15);
        warned.markIdleWarningIfDue(
                now,
                Duration.ofSeconds(20),
                Duration.ofSeconds(40)
        );
        givenIdleProperties();
        given(speakingQueuePersistenceService.warnCurrentSpeakerIfIdle(
                1L,
                now,
                Duration.ofSeconds(20),
                Duration.ofSeconds(40)
        )).willReturn(Optional.of(warned));

        Optional<SpeakingQueue> response =
                speakingQueueService.warnIdleCurrentSpeaker(1L, now);

        assertThat(response).contains(warned);
        verify(redisSpeakingQueueRepository, never())
                .removeCurrentSpeaker(anyLong(), anyLong());
        ArgumentCaptor<StageChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(StageChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type())
                .isEqualTo(StageEventType.SPEAKER_IDLE_WARNED);
        assertThat(eventCaptor.getValue().payload().status())
                .isEqualTo(SpeakingQueueStatus.ASSIGNED);
    }

    @Test
    void completeIdleCurrentSpeaker_removesRedisAndAssignsNextSpeaker() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 12, 11, 31, 40);
        SpeakingQueue completed = idleTimedOutCompletedRequest(1L, 7L, 15);
        givenIdleProperties();
        given(speakingQueuePersistenceService.completeCurrentSpeakerIfIdleTimedOut(
                1L,
                now,
                Duration.ofSeconds(20)
        )).willReturn(Optional.of(completed));
        given(speakingQueueProperties.getTurnDuration())
                .willReturn(Duration.ofMinutes(2));
        given(speakingQueuePersistenceService.assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).willReturn(SpeakingQueueAssignmentResult.empty());

        Optional<SpeakingQueue> response =
                speakingQueueService.completeIdleCurrentSpeaker(1L, now);

        assertThat(response).contains(completed);
        verify(redisSpeakingQueueRepository).removeCurrentSpeaker(1L, 7L);
        verify(speakingQueuePersistenceService).assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        ArgumentCaptor<StageChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(StageChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().type())
                .isEqualTo(StageEventType.SPEAKER_COMPLETED);
        assertThat(eventCaptor.getValue().payload().status())
                .isEqualTo(SpeakingQueueStatus.COMPLETED);
        assertThat(eventCaptor.getValue().payload().endReason())
                .isEqualTo(StageTurnEndReason.IDLE_TIMEOUT);
    }

    @Test
    void getCurrentSpeaker_returnsEmptyResponseWhenCurrentSpeakerDoesNotExist() {
        given(speakingQueuePersistenceService.findCurrentSpeaker(1L))
                .willReturn(Optional.empty());

        StageCurrentSpeakerRes response =
                speakingQueueService.getCurrentSpeaker(1L);

        assertThat(response.hasCurrentSpeaker()).isFalse();
        assertThat(response.currentSpeaker()).isNull();
    }

    @Test
    void getCurrentSpeaker_returnsCurrentSpeakerWithNickname() {
        CurrentSpeakerProjection currentSpeaker = currentSpeakerProjection(
                7L,
                "logic_hunter",
                SpeechStance.PRO,
                15,
                LocalDateTime.of(2026, 6, 12, 11, 31),
                LocalDateTime.of(2026, 6, 12, 11, 33)
        );
        given(speakingQueuePersistenceService.findCurrentSpeaker(1L))
                .willReturn(Optional.of(currentSpeaker));

        StageCurrentSpeakerRes response =
                speakingQueueService.getCurrentSpeaker(1L);

        assertThat(response.hasCurrentSpeaker()).isTrue();
        assertThat(response.currentSpeaker().userId()).isEqualTo(7L);
        assertThat(response.currentSpeaker().nickname()).isEqualTo("logic_hunter");
        assertThat(response.currentSpeaker().stance()).isEqualTo(SpeechStance.PRO);
        assertThat(response.currentSpeaker().queueOrder()).isEqualTo(15);
        assertThat(response.currentSpeaker().assignedAt())
                .isEqualTo(LocalDateTime.of(2026, 6, 12, 11, 31));
        assertThat(response.currentSpeaker().expiresAt())
                .isEqualTo(LocalDateTime.of(2026, 6, 12, 11, 33));
    }

    @Test
    void getQueueSummary_returnsFirstFiveWaitingSpeakers() {
        List<Long> userIds = List.of(10L, 20L);
        givenQueueProperties(5, 20, 100);
        given(redisSpeakingQueueRepository.count(1L)).willReturn(8L);
        given(redisSpeakingQueueRepository.findWaitingUserIds(1L, 0, 4))
                .willReturn(userIds);
        given(speakingQueuePersistenceService.findNicknamesByUserIds(userIds))
                .willReturn(Map.of(
                        10L, "logic_hunter",
                        20L, "dream_catcher"
                ));

        StageQueueRes response = speakingQueueService.getQueueSummary(1L);

        assertThat(response.totalWaitingCount()).isEqualTo(8L);
        assertThat(response.offset()).isZero();
        assertThat(response.size()).isEqualTo(5);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).rank()).isEqualTo(1);
        assertThat(response.items().get(0).userId()).isEqualTo(10L);
        assertThat(response.items().get(0).nickname()).isEqualTo("logic_hunter");
        assertThat(response.items().get(1).rank()).isEqualTo(2);
        assertThat(response.items().get(1).userId()).isEqualTo(20L);
        assertThat(response.items().get(1).nickname()).isEqualTo("dream_catcher");
    }

    @Test
    void getWaitingQueue_returnsPagedWaitingSpeakersWithRankOffset() {
        List<Long> userIds = List.of(30L, 40L);
        givenQueueProperties(5, 20, 100);
        given(redisSpeakingQueueRepository.count(1L)).willReturn(4L);
        given(redisSpeakingQueueRepository.findWaitingUserIds(1L, 2, 3))
                .willReturn(userIds);
        given(speakingQueuePersistenceService.findNicknamesByUserIds(userIds))
                .willReturn(Map.of(
                        30L, "neon_wave",
                        40L, "open_mind"
                ));

        StageQueueRes response = speakingQueueService.getWaitingQueue(1L, 2, 2);

        assertThat(response.totalWaitingCount()).isEqualTo(4L);
        assertThat(response.offset()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).rank()).isEqualTo(3);
        assertThat(response.items().get(0).nickname()).isEqualTo("neon_wave");
        assertThat(response.items().get(1).rank()).isEqualTo(4);
        assertThat(response.items().get(1).nickname()).isEqualTo("open_mind");
    }

    @Test
    void getWaitingQueue_returnsEmptyItemsWhenQueueIsEmpty() {
        givenQueueProperties(5, 20, 100);
        given(redisSpeakingQueueRepository.count(1L)).willReturn(0L);
        given(redisSpeakingQueueRepository.findWaitingUserIds(1L, 0, 19))
                .willReturn(List.of());
        given(speakingQueuePersistenceService.findNicknamesByUserIds(List.of()))
                .willReturn(Map.of());

        StageQueueRes response = speakingQueueService.getWaitingQueue(1L, 0, 20);

        assertThat(response.totalWaitingCount()).isZero();
        assertThat(response.hasNext()).isFalse();
        assertThat(response.items()).isEmpty();
    }

    @Test
    void getWaitingQueue_returnsDbQueueWhenRedisReadFails() {
        SpeakingQueue third = persistedWaitingRequest(1L, 30L, 3);
        SpeakingQueue fourth = persistedWaitingRequest(1L, 40L, 4);
        givenQueueProperties(5, 20, 100);
        given(redisSpeakingQueueRepository.count(1L))
                .willThrow(new IllegalStateException("redis unavailable"));
        given(speakingQueuePersistenceService.countWaitingRequests(1L))
                .willReturn(4L);
        given(speakingQueuePersistenceService.findWaitingRequestsForRedisReadFallback(
                1L,
                2,
                2
        )).willReturn(List.of(third, fourth));
        given(speakingQueuePersistenceService.findNicknamesByUserIds(List.of(30L, 40L)))
                .willReturn(Map.of(
                        30L, "neon_wave",
                        40L, "open_mind"
                ));

        StageQueueRes response = speakingQueueService.getWaitingQueue(1L, 2, 2);

        assertThat(response.totalWaitingCount()).isEqualTo(4L);
        assertThat(response.offset()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).rank()).isEqualTo(3);
        assertThat(response.items().get(0).userId()).isEqualTo(30L);
        assertThat(response.items().get(0).nickname()).isEqualTo("neon_wave");
        assertThat(response.items().get(1).rank()).isEqualTo(4);
        assertThat(response.items().get(1).userId()).isEqualTo(40L);
        assertThat(response.items().get(1).nickname()).isEqualTo("open_mind");
    }

    @Test
    void getWaitingQueue_usesConfiguredDefaultPageSizeWhenSizeIsMissing() {
        givenQueueProperties(5, 30, 100);
        given(redisSpeakingQueueRepository.count(1L)).willReturn(0L);
        given(redisSpeakingQueueRepository.findWaitingUserIds(1L, 0, 29))
                .willReturn(List.of());
        given(speakingQueuePersistenceService.findNicknamesByUserIds(List.of()))
                .willReturn(Map.of());

        StageQueueRes response = speakingQueueService.getWaitingQueue(1L, null, null);

        assertThat(response.offset()).isZero();
        assertThat(response.size()).isEqualTo(30);
        assertThat(response.items()).isEmpty();
    }

    @Test
    void getWaitingQueue_rejectsSizeGreaterThanConfiguredMaxPageSize() {
        givenQueueProperties(5, 20, 50);

        assertThatThrownBy(() -> speakingQueueService.getWaitingQueue(1L, 0, 51))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    private void givenQueueProperties(
            int summarySize,
            int defaultPageSize,
            int maxPageSize
    ) {
        SpeakingQueueProperties.Queue queue = new SpeakingQueueProperties.Queue();
        queue.setSummarySize(summarySize);
        queue.setDefaultPageSize(defaultPageSize);
        queue.setMaxPageSize(maxPageSize);
        given(speakingQueueProperties.getQueue()).willReturn(queue);
    }

    private void givenIdleProperties() {
        SpeakingQueueProperties.Idle idle = new SpeakingQueueProperties.Idle();
        idle.setWarningDelay(Duration.ofSeconds(20));
        idle.setTimeoutDelayAfterWarning(Duration.ofSeconds(20));
        idle.setWarningSuppressionBeforeExpiration(Duration.ofSeconds(40));
        given(speakingQueueProperties.getIdle()).willReturn(idle);
    }

    private CurrentSpeakerProjection currentSpeakerProjection(
            Long userId,
            String nickname,
            SpeechStance stance,
            Integer queueOrder,
            LocalDateTime assignedAt,
            LocalDateTime expiresAt
    ) {
        return new CurrentSpeakerProjection() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public String getNickname() {
                return nickname;
            }

            @Override
            public SpeechStance getStance() {
                return stance;
            }

            @Override
            public Integer getQueueOrder() {
                return queueOrder;
            }

            @Override
            public LocalDateTime getAssignedAt() {
                return assignedAt;
            }

            @Override
            public LocalDateTime getExpiresAt() {
                return expiresAt;
            }
        };
    }

    private SpeakingQueue persistedWaitingRequest(Long roomId, Long userId, int queueOrder) {
        return SpeakingQueue.waiting(
                roomId,
                userId,
                queueOrder,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
    }

    private SpeakingQueue completedRequest(Long roomId, Long userId, int queueOrder) {
        SpeakingQueue speakingQueue = assignedRequest(roomId, userId, queueOrder);
        speakingQueue.complete();
        return speakingQueue;
    }

    private SpeakingQueue idleTimedOutCompletedRequest(
            Long roomId,
            Long userId,
            int queueOrder
    ) {
        SpeakingQueue speakingQueue = assignedRequest(roomId, userId, queueOrder);
        speakingQueue.markIdleWarningIfDue(
                LocalDateTime.of(2026, 6, 12, 11, 31, 20),
                Duration.ofSeconds(20),
                Duration.ofSeconds(40)
        );
        speakingQueue.complete();
        return speakingQueue;
    }

    private SpeakingQueueAssignmentResult assignmentResult(SpeakingQueue assignedRequest) {
        return SpeakingQueueAssignmentResult.of(
                Optional.of(assignedRequest),
                List.of()
        );
    }

    private SpeakingQueue assignedRequest(Long roomId, Long userId, int queueOrder) {
        SpeakingQueue speakingQueue =
                persistedWaitingRequest(roomId, userId, queueOrder);
        speakingQueue.assign(
                LocalDateTime.of(2026, 6, 12, 11, 31),
                LocalDateTime.of(2026, 6, 12, 11, 33)
        );
        return speakingQueue;
    }

}
