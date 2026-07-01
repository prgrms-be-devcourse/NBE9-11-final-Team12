package com.sisibibi.api.domain.speech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.speech.dto.event.StageSummaryChangedEvent;
import com.sisibibi.api.domain.speech.dto.event.StageSummaryEventType;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.speech.config.StageSummaryProperties;
import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.StageSummary;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StageSummaryServiceTest {

    @Mock
    private StageSummaryPersistenceService stageSummaryPersistenceService;

    @Mock
    private StageSummaryGenerator stageSummaryGenerator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private StageSummaryProperties stageSummaryProperties;
    private StageSummaryService stageSummaryService;

    @BeforeEach
    void setUp() {
        stageSummaryProperties = new StageSummaryProperties();
        stageSummaryProperties.setGenerateTimeout(Duration.ofSeconds(10));
        stageSummaryProperties.setMinCompletedSpeakerCount(10);
        stageSummaryProperties.setMaxGenerationAttempts(3);
        stageSummaryService = createService(Runnable::run);
    }

    @Test
    void generateIfNeeded_completesSummary_whenAiGenerationSucceeds() {
        Room room = room();
        Speech speech = Speech.createMainOpinion(1L, 7L, "토론 의견", SpeechStance.PRO);
        StageSummaryGenerationContext context =
                StageSummaryGenerationContext.callAi(77L, room, List.of(speech));
        StageSummaryResult result = new StageSummaryResult(
                "지금까지는 양측의 쟁점이 정리되고 있습니다.",
                List.of("접근성", "안전성", "책임 소재")
        );
        StageSummary completedSummary = StageSummary.pending(
                1L,
                LocalDateTime.of(2026, 6, 26, 11, 0),
                10
        );
        ReflectionTestUtils.setField(completedSummary, "id", 77L);
        completedSummary.complete(
                result.moderatorSummary(),
                result.keyPoints(),
                1,
                LocalDateTime.of(2026, 6, 26, 11, 1)
        );
        given(stageSummaryPersistenceService.prepareGeneration(
                eq(1L),
                any(LocalDateTime.class),
                eq(10),
                eq(3),
                eq(Duration.ofSeconds(10))
        )).willReturn(context);
        given(stageSummaryGenerator.generate(room, List.of(speech))).willReturn(result);
        given(stageSummaryPersistenceService.complete(
                eq(77L),
                eq(result),
                eq(1),
                any(LocalDateTime.class)
        )).willReturn(completedSummary);

        stageSummaryService.generateIfNeeded(1L);

        verify(stageSummaryPersistenceService).complete(
                eq(77L),
                eq(result),
                eq(1),
                any(LocalDateTime.class)
        );
        ArgumentCaptor<StageSummaryChangedEvent> eventCaptor =
                ArgumentCaptor.forClass(StageSummaryChangedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        StageSummaryChangedEvent event = eventCaptor.getValue();
        assertThat(event.type()).isEqualTo(StageSummaryEventType.STAGE_SUMMARY_COMPLETED);
        assertThat(event.roomId()).isEqualTo(1L);
        assertThat(event.payload().summaryId()).isEqualTo(77L);
        assertThat(event.payload().roomId()).isEqualTo(1L);
        verify(stageSummaryPersistenceService, never()).fail(any(), any());
    }

    @Test
    void generateIfNeeded_skipsAiCall_whenPersistenceContextSaysSkip() {
        given(stageSummaryPersistenceService.prepareGeneration(
                eq(1L),
                any(LocalDateTime.class),
                eq(10),
                eq(3),
                eq(Duration.ofSeconds(10))
        )).willReturn(StageSummaryGenerationContext.skip());

        stageSummaryService.generateIfNeeded(1L);

        verify(stageSummaryGenerator, never()).generate(any(), any());
        verify(stageSummaryPersistenceService, never()).complete(any(), any(), any(Integer.class), any());
        verify(stageSummaryPersistenceService, never()).fail(any(), any());
    }

    @Test
    void generateIfNeeded_failsSummary_whenAiResultHasBlankSummary() {
        Room room = room();
        StageSummaryGenerationContext context =
                StageSummaryGenerationContext.callAi(77L, room, List.of());
        given(stageSummaryPersistenceService.prepareGeneration(
                eq(1L),
                any(LocalDateTime.class),
                eq(10),
                eq(3),
                eq(Duration.ofSeconds(10))
        )).willReturn(context);
        given(stageSummaryGenerator.generate(room, List.of()))
                .willReturn(new StageSummaryResult("", List.of("쟁점 1", "쟁점 2", "쟁점 3")));

        stageSummaryService.generateIfNeeded(1L);

        verify(stageSummaryPersistenceService).fail(eq(77L), any(String.class));
        verify(stageSummaryPersistenceService, never()).complete(any(), any(), any(Integer.class), any());
    }

    @Test
    void generateIfNeeded_failsSummary_whenAiResultDoesNotHaveExactlyThreeKeyPoints() {
        Room room = room();
        StageSummaryGenerationContext context =
                StageSummaryGenerationContext.callAi(77L, room, List.of());
        given(stageSummaryPersistenceService.prepareGeneration(
                eq(1L),
                any(LocalDateTime.class),
                eq(10),
                eq(3),
                eq(Duration.ofSeconds(10))
        )).willReturn(context);
        given(stageSummaryGenerator.generate(room, List.of()))
                .willReturn(new StageSummaryResult("중간 정리", List.of("쟁점 1", "쟁점 2")));

        stageSummaryService.generateIfNeeded(1L);

        verify(stageSummaryPersistenceService).fail(eq(77L), any(String.class));
        verify(stageSummaryPersistenceService, never()).complete(any(), any(), any(Integer.class), any());
    }

    @Test
    void generateIfNeeded_failsSummary_whenAiGenerationThrowsException() {
        Room room = room();
        StageSummaryGenerationContext context =
                StageSummaryGenerationContext.callAi(77L, room, List.of());
        given(stageSummaryPersistenceService.prepareGeneration(
                eq(1L),
                any(LocalDateTime.class),
                eq(10),
                eq(3),
                eq(Duration.ofSeconds(10))
        )).willReturn(context);
        given(stageSummaryGenerator.generate(room, List.of()))
                .willThrow(new IllegalStateException("temporary api failure"));

        stageSummaryService.generateIfNeeded(1L);

        verify(stageSummaryPersistenceService).fail(eq(77L), eq("temporary api failure"));
        verify(stageSummaryPersistenceService, never()).complete(any(), any(), any(Integer.class), any());
    }

    @Test
    void generateIfNeeded_failsSummary_whenAiGenerationTimesOut() {
        Room room = room();
        StageSummaryGenerationContext context =
                StageSummaryGenerationContext.callAi(77L, room, List.of());
        stageSummaryProperties.setGenerateTimeout(Duration.ofMillis(1));
        stageSummaryService = createService(command -> {
        });
        given(stageSummaryPersistenceService.prepareGeneration(
                eq(1L),
                any(LocalDateTime.class),
                eq(10),
                eq(3),
                eq(Duration.ofMillis(1))
        )).willReturn(context);

        stageSummaryService.generateIfNeeded(1L);

        verify(stageSummaryPersistenceService).fail(eq(77L), any(String.class));
        verify(stageSummaryPersistenceService, never()).complete(any(), any(), any(Integer.class), any());
    }

    @Test
    void generateIfNeeded_cancelsPendingGeneration_whenInterrupted() {
        Room room = room();
        StageSummaryGenerationContext context =
                StageSummaryGenerationContext.callAi(77L, room, List.of());
        AtomicReference<Runnable> submittedTask = new AtomicReference<>();
        stageSummaryService = createService(submittedTask::set);
        given(stageSummaryPersistenceService.prepareGeneration(
                eq(1L),
                any(LocalDateTime.class),
                eq(10),
                eq(3),
                eq(Duration.ofSeconds(10))
        )).willReturn(context);

        Thread.currentThread().interrupt();
        try {
            stageSummaryService.generateIfNeeded(1L);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }

        submittedTask.get().run();

        verify(stageSummaryPersistenceService).fail(eq(77L), any(String.class));
        verify(stageSummaryGenerator, never()).generate(any(), any());
        verify(stageSummaryPersistenceService, never()).complete(any(), any(), any(Integer.class), any());
    }

    private StageSummaryService createService(Executor executor) {
        return new StageSummaryService(
                stageSummaryPersistenceService,
                stageSummaryGenerator,
                eventPublisher,
                executor,
                stageSummaryProperties
        );
    }

    private Room room() {
        return Room.open(
                1L,
                "토론방 제목",
                LocalDateTime.of(2026, 6, 26, 10, 0),
                LocalDateTime.of(2026, 6, 26, 12, 0),
                100
        );
    }
}
