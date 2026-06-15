package com.sisibibi.api.domain.speech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.speech.config.SpeakingQueueProperties;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.repository.RedisSpeakingQueueRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpeakingQueueServiceTest {

    @Mock
    private RedisSpeakingQueueRepository redisSpeakingQueueRepository;

    @Mock
    private SpeakingQueuePersistenceService speakingQueuePersistenceService;

    @Mock
    private SpeakingQueueProperties speakingQueueProperties;

    @InjectMocks
    private SpeakingQueueService speakingQueueService;

    @Test
    void requestSpeakingTurn_persistsWaitingRequestWithoutSynchronousAssignment() {
        SpeakingQueue saved = persistedWaitingRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.createWaitingRequest(1L, 7L))
                .willReturn(saved);

        StageRequestRes response = speakingQueueService.requestSpeakingTurn(1L, 7L);

        assertThat(response.roomId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(response.queueOrder()).isEqualTo(15);
        verify(redisSpeakingQueueRepository).upsert(1L, 7L, 15);
        verify(redisSpeakingQueueRepository, never()).assign(1L, 7L);
        verify(speakingQueuePersistenceService, never()).assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
    }

    @Test
    void requestSpeakingTurn_doesNotWriteRedisWhenRdbPersistenceFails() {
        given(speakingQueuePersistenceService.createWaitingRequest(1L, 7L))
                .willThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> speakingQueueService.requestSpeakingTurn(1L, 7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        verify(redisSpeakingQueueRepository, never())
                .upsert(org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void requestSpeakingTurn_keepsDurableRequestWhenRedisSynchronizationFails() {
        SpeakingQueue saved = persistedWaitingRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.createWaitingRequest(1L, 7L))
                .willReturn(saved);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisSpeakingQueueRepository)
                .upsert(1L, 7L, 15);

        StageRequestRes response = speakingQueueService.requestSpeakingTurn(1L, 7L);

        assertThat(response.queueOrder()).isEqualTo(15);
        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        verify(speakingQueuePersistenceService).createWaitingRequest(1L, 7L);
        verify(speakingQueuePersistenceService, never()).assignNextSpeaker(
                eq(1L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
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
        )).willReturn(Optional.empty());

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
    }

    @Test
    void cancelSpeakingRequest_cancelsDurableRequestAndRemovesRedisProjection() {
        SpeakingQueue canceled = persistedWaitingRequest(1L, 7L, 15);
        canceled.cancel(LocalDateTime.of(2026, 6, 12, 11, 35));
        given(speakingQueuePersistenceService.cancelWaitingRequest(1L, 7L))
                .willReturn(canceled);

        speakingQueueService.cancelSpeakingRequest(1L, 7L);

        verify(speakingQueuePersistenceService).cancelWaitingRequest(1L, 7L);
        verify(redisSpeakingQueueRepository).remove(1L, 7L);
    }

    @Test
    void cancelSpeakingRequest_keepsCanceledRdbStateWhenRedisRemovalFails() {
        SpeakingQueue canceled = persistedWaitingRequest(1L, 7L, 15);
        canceled.cancel(LocalDateTime.of(2026, 6, 12, 11, 35));
        given(speakingQueuePersistenceService.cancelWaitingRequest(1L, 7L))
                .willReturn(canceled);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisSpeakingQueueRepository)
                .remove(1L, 7L);

        speakingQueueService.cancelSpeakingRequest(1L, 7L);

        verify(speakingQueuePersistenceService).cancelWaitingRequest(1L, 7L);
        verify(redisSpeakingQueueRepository).remove(1L, 7L);
    }

    @Test
    void completeSpeakingTurn_completesDurableRequestAndRemovesCurrentSpeaker() {
        SpeakingQueue completed = completedRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.completeCurrentSpeaker(1L, 7L))
                .willReturn(completed);

        speakingQueueService.completeSpeakingTurn(1L, 7L);

        verify(speakingQueuePersistenceService).completeCurrentSpeaker(1L, 7L);
        verify(redisSpeakingQueueRepository).removeCurrentSpeaker(1L, 7L);
    }

    @Test
    void completeSpeakingTurn_keepsCompletedRdbStateWhenRedisRemovalFails() {
        SpeakingQueue completed = completedRequest(1L, 7L, 15);
        given(speakingQueuePersistenceService.completeCurrentSpeaker(1L, 7L))
                .willReturn(completed);
        doThrow(new IllegalStateException("redis unavailable"))
                .when(redisSpeakingQueueRepository)
                .removeCurrentSpeaker(1L, 7L);

        speakingQueueService.completeSpeakingTurn(1L, 7L);

        verify(speakingQueuePersistenceService).completeCurrentSpeaker(1L, 7L);
        verify(redisSpeakingQueueRepository).removeCurrentSpeaker(1L, 7L);
    }

    private SpeakingQueue persistedWaitingRequest(Long roomId, Long userId, int queueOrder) {
        return SpeakingQueue.waiting(
                roomId,
                userId,
                queueOrder,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
    }

    private SpeakingQueue completedRequest(Long roomId, Long userId, int queueOrder) {
        SpeakingQueue speakingQueue =
                persistedWaitingRequest(roomId, userId, queueOrder);
        speakingQueue.assign(
                LocalDateTime.of(2026, 6, 12, 11, 31),
                LocalDateTime.of(2026, 6, 12, 11, 33)
        );
        speakingQueue.complete();
        return speakingQueue;
    }

}
