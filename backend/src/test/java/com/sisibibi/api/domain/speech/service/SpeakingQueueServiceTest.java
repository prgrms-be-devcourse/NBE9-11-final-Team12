package com.sisibibi.api.domain.speech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.repository.RedisSpeakingQueueRepository;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.OptionalInt;

@ExtendWith(MockitoExtension.class)
class SpeakingQueueServiceTest {

    @Mock
    private RedisSpeakingQueueRepository speakingQueueRepository;

    @Mock
    private SpeakingQueueRepository speakingQueueJpaRepository;

    @InjectMocks
    private SpeakingQueueService speakingQueueService;

    @Test
    void requestSpeakingTurn_persistsWaitingRequestAndReturnsQueueOrder() {
        given(speakingQueueJpaRepository.existsByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        )).willReturn(false);
        given(speakingQueueRepository.enqueue(1L, 7L)).willReturn(OptionalInt.of(3));
        given(speakingQueueJpaRepository.saveAndFlush(
                org.mockito.ArgumentMatchers.any(SpeakingQueue.class)
        )).willAnswer(invocation -> invocation.getArgument(0));

        StageRequestRes response = speakingQueueService.requestSpeakingTurn(1L, 7L);

        assertThat(response.roomId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.status()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(response.queueOrder()).isEqualTo(3);

        ArgumentCaptor<SpeakingQueue> captor = ArgumentCaptor.forClass(SpeakingQueue.class);
        verify(speakingQueueJpaRepository).saveAndFlush(captor.capture());

        SpeakingQueue saved = captor.getValue();
        assertThat(saved.getRoomId()).isEqualTo(1L);
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getQueueOrder()).isEqualTo(3);
        assertThat(saved.getStatus()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(saved.getRequestedAt()).isNotNull();
    }

    @Test
    void requestSpeakingTurn_rejectsDuplicateRequestRecordedInRdb() {
        given(speakingQueueJpaRepository.existsByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        )).willReturn(true);

        assertThatThrownBy(() -> speakingQueueService.requestSpeakingTurn(1L, 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS);

        verify(speakingQueueRepository, never()).enqueue(1L, 7L);
    }

    @Test
    void requestSpeakingTurn_rejectsConcurrentDuplicateDetectedByRedis() {
        given(speakingQueueJpaRepository.existsByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        )).willReturn(false);
        given(speakingQueueRepository.enqueue(1L, 7L)).willReturn(OptionalInt.empty());

        assertThatThrownBy(() -> speakingQueueService.requestSpeakingTurn(1L, 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS);

        verify(speakingQueueJpaRepository, never())
                .saveAndFlush(org.mockito.ArgumentMatchers.any(SpeakingQueue.class));
    }

    @Test
    void requestSpeakingTurn_removesRedisEntryWhenRdbSaveFails() {
        given(speakingQueueJpaRepository.existsByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        )).willReturn(false);
        given(speakingQueueRepository.enqueue(1L, 7L)).willReturn(OptionalInt.of(3));
        given(speakingQueueJpaRepository.saveAndFlush(
                org.mockito.ArgumentMatchers.any(SpeakingQueue.class)
        )).willThrow(new IllegalStateException("database unavailable"));

        assertThatThrownBy(() -> speakingQueueService.requestSpeakingTurn(1L, 7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        verify(speakingQueueRepository).remove(1L, 7L);
    }
}
