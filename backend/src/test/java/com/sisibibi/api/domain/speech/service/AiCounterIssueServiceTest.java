package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.config.SpeechAiGenerator;
import com.sisibibi.api.domain.speech.dto.event.AiCounterIssueChangedEvent;
import com.sisibibi.api.domain.speech.dto.event.AiCounterIssueEventType;
import com.sisibibi.api.domain.speech.entity.AiCounterIssue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.util.SpeakingStreakPolicy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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

    @InjectMocks
    private AiCounterIssueService aiCounterIssueService;

    @Test
    void suggestIfNeeded_publishesEvent_whenAiCounterIssueIsCompleted() {
        Long roomId = 1L;
        SpeakingQueue first = assignedQueue(30L, SpeechStance.PRO);
        SpeakingQueue second = assignedQueue(29L, SpeechStance.PRO);
        SpeakingQueue third = assignedQueue(28L, SpeechStance.PRO);
        AiCounterIssue pending = AiCounterIssue.pending(roomId, 30L, SpeechStance.CON);
        ReflectionTestUtils.setField(pending, "id", 11L);
        AiCounterIssue completed = AiCounterIssue.pending(roomId, 30L, SpeechStance.CON);
        ReflectionTestUtils.setField(completed, "id", 11L);
        ReflectionTestUtils.setField(completed, "createdAt",
                LocalDateTime.of(2026, 6, 25, 14, 0));
        completed.complete(
                "반대 측에서 검토할 핵심 쟁점입니다.",
                LocalDateTime.of(2026, 6, 25, 14, 1)
        );
        Room room = Room.open(
                1L,
                "AI 규제 토론",
                LocalDateTime.of(2026, 6, 25, 13, 0),
                LocalDateTime.of(2026, 6, 25, 15, 0),
                100
        );

        given(speakingQueueRepository
                .findTop3ByRoomIdAndStatusInAndStanceIsNotNullOrderByAssignedAtDesc(
                        roomId,
                        List.of(SpeakingQueueStatus.ASSIGNED, SpeakingQueueStatus.COMPLETED)
                ))
                .willReturn(List.of(first, second, third));
        given(aiCounterIssuePersistenceService.createPendingIfAbsent(
                roomId,
                30L,
                SpeechStance.CON
        )).willReturn(Optional.of(pending));
        given(roomRepository.findById(roomId)).willReturn(Optional.of(room));
        given(aiCounterIssueGenerator.generate(room, SpeechStance.CON))
                .willReturn("반대 측에서 검토할 핵심 쟁점입니다.");
        given(aiCounterIssuePersistenceService.complete(
                11L,
                "반대 측에서 검토할 핵심 쟁점입니다."
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
        assertThat(event.payload().content()).isEqualTo("반대 측에서 검토할 핵심 쟁점입니다.");
    }

    private SpeakingQueue assignedQueue(Long userId, SpeechStance stance) {
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
        return queue;
    }
}
