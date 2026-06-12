package com.sisibibi.api.domain.speech.scheduler;

import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpeakingQueueExpirationSchedulerTest {

    @Mock
    private SpeakingQueueRepository speakingQueueRepository;

    @Mock
    private SpeakingQueueService speakingQueueService;

    @InjectMocks
    private SpeakingQueueExpirationScheduler scheduler;

    @Test
    void expireTimedOutSpeakers_scansExpiredAssignedRoomsAndDelegatesToService() {
        ReflectionTestUtils.setField(scheduler, "speakingTimeLimitSeconds", 300L);
        given(speakingQueueRepository.findDistinctRoomIdsByStatusAndAssignedAtLessThanEqual(
                eq(SpeakingQueueStatus.ASSIGNED),
                any(LocalDateTime.class)
        )).willReturn(List.of(1L, 2L));

        scheduler.expireTimedOutSpeakers();

        ArgumentCaptor<LocalDateTime> expiresBeforeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(speakingQueueRepository).findDistinctRoomIdsByStatusAndAssignedAtLessThanEqual(
                eq(SpeakingQueueStatus.ASSIGNED),
                expiresBeforeCaptor.capture()
        );
        assertThat(expiresBeforeCaptor.getValue()).isBeforeOrEqualTo(LocalDateTime.now());
        verify(speakingQueueService).expireCurrentSpeakerIfTimedOut(
                eq(1L),
                any(LocalDateTime.class),
                eq(Duration.ofSeconds(300))
        );
        verify(speakingQueueService).expireCurrentSpeakerIfTimedOut(
                eq(2L),
                any(LocalDateTime.class),
                eq(Duration.ofSeconds(300))
        );
    }

    @Test
    void expireTimedOutSpeakers_continuesWhenOneRoomIsAlreadyHandled() {
        ReflectionTestUtils.setField(scheduler, "speakingTimeLimitSeconds", 300L);
        given(speakingQueueRepository.findDistinctRoomIdsByStatusAndAssignedAtLessThanEqual(
                eq(SpeakingQueueStatus.ASSIGNED),
                any(LocalDateTime.class)
        )).willReturn(List.of(1L, 2L));
        willThrow(new CustomException(ErrorCode.SPEAKING_TIME_NOT_EXPIRED))
                .given(speakingQueueService)
                .expireCurrentSpeakerIfTimedOut(
                        eq(1L),
                        any(LocalDateTime.class),
                        eq(Duration.ofSeconds(300))
                );

        scheduler.expireTimedOutSpeakers();

        verify(speakingQueueService).expireCurrentSpeakerIfTimedOut(
                eq(2L),
                any(LocalDateTime.class),
                eq(Duration.ofSeconds(300))
        );
    }
}
