package com.sisibibi.api.domain.speech.scheduler;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpeakingQueueExpirationSchedulerTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 15, 17, 30);

    @Mock
    private SpeakingQueueRepository speakingQueueRepository;

    @Mock
    private SpeakingQueueService speakingQueueService;

    @InjectMocks
    private SpeakingQueueExpirationScheduler speakingQueueExpirationScheduler;

    @Test
    void expireSpeakersAt_requestsExpirationForEachCandidateRoom() {
        given(speakingQueueRepository.findRoomIdsWithExpiredSpeaker(NOW))
                .willReturn(List.of(1L, 2L));

        speakingQueueExpirationScheduler.expireSpeakersAt(NOW);

        verify(speakingQueueService).expireCurrentSpeaker(1L, NOW);
        verify(speakingQueueService).expireCurrentSpeaker(2L, NOW);
    }

    @Test
    void expireSpeakersAt_doesNothingWhenThereIsNoCandidateRoom() {
        given(speakingQueueRepository.findRoomIdsWithExpiredSpeaker(NOW))
                .willReturn(List.of());

        speakingQueueExpirationScheduler.expireSpeakersAt(NOW);

        verify(speakingQueueService, never())
                .expireCurrentSpeaker(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any(LocalDateTime.class)
                );
    }

    @Test
    void expireSpeakersAt_continuesWhenOneRoomFails() {
        given(speakingQueueRepository.findRoomIdsWithExpiredSpeaker(NOW))
                .willReturn(List.of(1L, 2L));
        willThrow(new IllegalStateException("database unavailable"))
                .given(speakingQueueService)
                .expireCurrentSpeaker(1L, NOW);

        speakingQueueExpirationScheduler.expireSpeakersAt(NOW);

        verify(speakingQueueService).expireCurrentSpeaker(1L, NOW);
        verify(speakingQueueService).expireCurrentSpeaker(2L, NOW);
    }
}
