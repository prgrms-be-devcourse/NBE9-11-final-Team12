package com.sisibibi.api.domain.speech.scheduler;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.speech.config.SpeakingQueueProperties;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.service.SpeakingQueueService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpeakingQueueIdleSchedulerTest {

    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 15, 17, 30);

    @Mock
    private SpeakingQueueRepository speakingQueueRepository;

    @Mock
    private SpeakingQueueService speakingQueueService;

    @Mock
    private SpeakingQueueProperties speakingQueueProperties;

    @InjectMocks
    private SpeakingQueueIdleScheduler speakingQueueIdleScheduler;

    @Test
    void handleIdleSpeakersAt_warnsAndCompletesCandidateRooms() {
        SpeakingQueueProperties.Idle idleProperties = idleProperties();
        given(speakingQueueProperties.getIdle()).willReturn(idleProperties);
        given(speakingQueueRepository.findRoomIdsRequiringIdleWarning(
                NOW,
                Duration.ofSeconds(20),
                Duration.ofSeconds(40)
        )).willReturn(List.of(1L));
        given(speakingQueueRepository.findRoomIdsWithIdleTimedOutSpeaker(
                NOW,
                Duration.ofSeconds(20)
        )).willReturn(List.of(2L));

        speakingQueueIdleScheduler.handleIdleSpeakersAt(NOW);

        verify(speakingQueueService).warnIdleCurrentSpeaker(1L, NOW);
        verify(speakingQueueService).completeIdleCurrentSpeaker(2L, NOW);
    }

    @Test
    void handleIdleSpeakersAt_continuesWhenOneRoomFails() {
        SpeakingQueueProperties.Idle idleProperties = idleProperties();
        given(speakingQueueProperties.getIdle()).willReturn(idleProperties);
        given(speakingQueueRepository.findRoomIdsRequiringIdleWarning(
                NOW,
                Duration.ofSeconds(20),
                Duration.ofSeconds(40)
        )).willReturn(List.of(1L, 2L));
        given(speakingQueueRepository.findRoomIdsWithIdleTimedOutSpeaker(
                NOW,
                Duration.ofSeconds(20)
        )).willReturn(List.of());
        willThrow(new IllegalStateException("database unavailable"))
                .given(speakingQueueService)
                .warnIdleCurrentSpeaker(1L, NOW);

        speakingQueueIdleScheduler.handleIdleSpeakersAt(NOW);

        verify(speakingQueueService).warnIdleCurrentSpeaker(1L, NOW);
        verify(speakingQueueService).warnIdleCurrentSpeaker(2L, NOW);
    }

    private SpeakingQueueProperties.Idle idleProperties() {
        SpeakingQueueProperties.Idle idle = new SpeakingQueueProperties.Idle();
        idle.setWarningDelay(Duration.ofSeconds(20));
        idle.setTimeoutDelayAfterWarning(Duration.ofSeconds(20));
        idle.setWarningSuppressionBeforeExpiration(Duration.ofSeconds(40));
        return idle;
    }
}
