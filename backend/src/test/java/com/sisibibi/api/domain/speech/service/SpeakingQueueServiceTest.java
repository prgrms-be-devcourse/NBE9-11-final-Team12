package com.sisibibi.api.domain.speech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.repository.RedisSpeakingQueueRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpeakingQueueServiceTest {

    @Mock
    private RedisSpeakingQueueRepository redisSpeakingQueueRepository;

    @Mock
    private SpeakingQueuePersistenceService speakingQueuePersistenceService;

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
        verify(speakingQueuePersistenceService, never()).assignNextSpeaker(1L);
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
        verify(speakingQueuePersistenceService, never()).assignNextSpeaker(1L);
    }

    private SpeakingQueue persistedWaitingRequest(Long roomId, Long userId, int queueOrder) {
        return SpeakingQueue.waiting(
                roomId,
                userId,
                queueOrder,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
    }

}
