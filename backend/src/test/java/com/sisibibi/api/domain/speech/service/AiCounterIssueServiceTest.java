package com.sisibibi.api.domain.speech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.config.AiCounterIssueProperties;
import com.sisibibi.api.domain.speech.config.SpeechAiGenerator;
import com.sisibibi.api.domain.speech.dto.event.AiCounterIssueChangedEvent;
import com.sisibibi.api.domain.speech.dto.event.AiCounterIssueEventType;
import com.sisibibi.api.domain.speech.entity.AiCounterIssue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.util.SpeakingStreakPolicy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AiCounterIssueServiceTest {

    @Mock
    private SpeakingQueueRepository speakingQueueRepository;

    @Mock
    private AiCounterIssuePersistenceService aiCounterIssuePersistenceService;

    @Mock
    private RoomRepository roomRepository;

    @Spy
    private SpeakingStreakPolicy speakingStreakPolicy;

    @Mock
    private SpeechAiGenerator aiCounterIssueGenerator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private AiCounterIssueProperties aiCounterIssueProperties;
    private AiCounterIssueService aiCounterIssueService;

    @BeforeEach
    void setUp() {
        aiCounterIssueProperties = new AiCounterIssueProperties();
        aiCounterIssueProperties.setGenerateTimeout(Duration.ofSeconds(10));
        aiCounterIssueService = createService(Runnable::run);
    }

    @Test
    void suggestIfNeeded_publishesEvent_whenAiCounterIssueIsCompleted() {
        Long roomId = 1L;
        SpeakingQueue first = completedQueue(30L, SpeechStance.PRO);
        SpeakingQueue second = completedQueue(29L, SpeechStance.PRO);
        SpeakingQueue third = completedQueue(28L, SpeechStance.PRO);
        AiCounterIssue pending = AiCounterIssue.pending(roomId, 30L, SpeechStance.CON);
        ReflectionTestUtils.setField(pending, "id", 11L);
        AiCounterIssue completed = AiCounterIssue.pending(roomId, 30L, SpeechStance.CON);
        ReflectionTestUtils.setField(completed, "id", 11L);
        ReflectionTestUtils.setField(completed, "createdAt",
                LocalDateTime.of(2026, 6, 25, 14, 0));
        completed.complete(
                "Counter issue for the opposing side.",
                LocalDateTime.of(2026, 6, 25, 14, 1)
        );
        Room room = Room.open(
                1L,
                "AI debate topic",
                LocalDateTime.of(2026, 6, 25, 13, 0),
                LocalDateTime.of(2026, 6, 25, 15, 0),
                100
        );

        given(speakingQueueRepository
                .findTop3ByRoomIdAndStatusInAndStanceIsNotNullOrderByAssignedAtDesc(
                        roomId,
                        List.of(SpeakingQueueStatus.COMPLETED)
                ))
                .willReturn(List.of(first, second, third));
        given(aiCounterIssuePersistenceService.createPendingIfAbsent(
                roomId,
                30L,
                SpeechStance.CON
        )).willReturn(Optional.of(pending));
        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(aiCounterIssueGenerator.generate(room, SpeechStance.CON))
                .willReturn("Counter issue for the opposing side.");
        given(aiCounterIssuePersistenceService.complete(
                11L,
                "Counter issue for the opposing side."
        )).willReturn(completed);
        ArgumentCaptor<AiCounterIssueChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(AiCounterIssueChangedEvent.class);

        aiCounterIssueService.suggestIfNeeded(roomId);

        verify(eventPublisher).publishEvent(eventCaptor.capture());
        AiCounterIssueChangedEvent event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo(AiCounterIssueEventType.AI_COUNTER_ISSUE_SUGGESTED);
        assertThat(event.roomId()).isEqualTo(roomId);
        assertThat(event.payload().issueId()).isEqualTo(11L);
        assertThat(event.payload().targetStance()).isEqualTo(SpeechStance.CON);
        assertThat(event.payload().content()).isEqualTo("Counter issue for the opposing side.");
    }

    @Test
    void suggestIfNeeded_usesCompletedAssignmentsOnly() {
        given(speakingQueueRepository
                .findTop3ByRoomIdAndStatusInAndStanceIsNotNullOrderByAssignedAtDesc(
                        1L,
                        List.of(SpeakingQueueStatus.COMPLETED)
                ))
                .willReturn(List.of());

        aiCounterIssueService.suggestIfNeeded(1L);

        verify(aiCounterIssuePersistenceService, never())
                .createPendingIfAbsent(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void suggestIfNeeded_marksIssueAsFailed_whenAiGenerationTimesOut() {
        Long roomId = 1L;
        SpeakingQueue first = completedQueue(30L, SpeechStance.PRO);
        SpeakingQueue second = completedQueue(29L, SpeechStance.PRO);
        SpeakingQueue third = completedQueue(28L, SpeechStance.PRO);
        AiCounterIssue pending = AiCounterIssue.pending(roomId, 30L, SpeechStance.CON);
        ReflectionTestUtils.setField(pending, "id", 11L);
        Room room = Room.open(
                1L,
                "AI debate topic",
                LocalDateTime.of(2026, 6, 25, 13, 0),
                LocalDateTime.of(2026, 6, 25, 15, 0),
                100
        );
        aiCounterIssueProperties.setGenerateTimeout(Duration.ofMillis(1));
        aiCounterIssueService = createService(command -> {
        });

        given(speakingQueueRepository
                .findTop3ByRoomIdAndStatusInAndStanceIsNotNullOrderByAssignedAtDesc(
                        roomId,
                        List.of(SpeakingQueueStatus.COMPLETED)
                ))
                .willReturn(List.of(first, second, third));
        given(aiCounterIssuePersistenceService.createPendingIfAbsent(
                roomId,
                30L,
                SpeechStance.CON
        )).willReturn(Optional.of(pending));
        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        ArgumentCaptor<String> failureMessageCaptor = ArgumentCaptor.forClass(String.class);

        aiCounterIssueService.suggestIfNeeded(roomId);

        verify(aiCounterIssuePersistenceService)
                .fail(org.mockito.ArgumentMatchers.eq(11L), failureMessageCaptor.capture());
        assertThat(failureMessageCaptor.getValue())
                .contains("AI counter issue generation timed out");
        verify(aiCounterIssuePersistenceService, never())
                .complete(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyString()
                );
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any());
    }

    private AiCounterIssueService createService(Executor executor) {
        return new AiCounterIssueService(
                speakingQueueRepository,
                aiCounterIssuePersistenceService,
                roomRepository,
                speakingStreakPolicy,
                aiCounterIssueGenerator,
                eventPublisher,
                executor,
                aiCounterIssueProperties
        );
    }

    private SpeakingQueue completedQueue(Long userId, SpeechStance stance) {
        SpeakingQueue queue = SpeakingQueue.waiting(
                1L,
                userId,
                userId.intValue(),
                stance,
                LocalDateTime.of(2026, 6, 25, 13, 0)
        );
        ReflectionTestUtils.setField(queue, "id", userId);
        queue.assign(
                LocalDateTime.of(2026, 6, 25, 13, 10),
                LocalDateTime.of(2026, 6, 25, 13, 13)
        );
        queue.complete();
        return queue;
    }
}
