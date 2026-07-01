package com.sisibibi.api.domain.speech.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.StageSummary;
import com.sisibibi.api.domain.speech.entity.StageSummaryStatus;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speech.repository.StageSummaryRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StageSummaryPersistenceServiceTest {

    private static final int MIN_COMPLETED_SPEAKER_COUNT = 10;
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration GENERATE_TIMEOUT = Duration.ofSeconds(10);

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private SpeakingQueueRepository speakingQueueRepository;

    @Mock
    private SpeechRepository speechRepository;

    @Mock
    private StageSummaryRepository stageSummaryRepository;

    @InjectMocks
    private StageSummaryPersistenceService stageSummaryPersistenceService;

    @Test
    void prepareGeneration_skipsBeforeRoomMidpoint() {
        Room room = room(
                1L,
                LocalDateTime.of(2026, 6, 26, 10, 0),
                LocalDateTime.of(2026, 6, 26, 12, 0)
        );
        LocalDateTime beforeMidpoint = LocalDateTime.of(2026, 6, 26, 10, 59);
        given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));

        StageSummaryGenerationContext context = stageSummaryPersistenceService.prepareGeneration(
                1L,
                beforeMidpoint,
                MIN_COMPLETED_SPEAKER_COUNT,
                MAX_ATTEMPTS
        );

        assertThat(context.shouldCallAi()).isFalse();
        verify(speakingQueueRepository, never()).countDistinctCompletedSpeakersByRoomId(any());
        verify(stageSummaryRepository, never()).save(any());
        verify(stageSummaryRepository, never()).saveAndFlush(any());
    }

    @Test
    void prepareGeneration_skipsWhenCompletedSpeakerCountIsBelowThreshold() {
        Room room = room(
                1L,
                LocalDateTime.of(2026, 6, 26, 10, 0),
                LocalDateTime.of(2026, 6, 26, 12, 0)
        );
        LocalDateTime midpoint = LocalDateTime.of(2026, 6, 26, 11, 0);
        given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));
        given(speakingQueueRepository.countDistinctCompletedSpeakersByRoomId(1L))
                .willReturn(9L);

        StageSummaryGenerationContext context = stageSummaryPersistenceService.prepareGeneration(
                1L,
                midpoint,
                MIN_COMPLETED_SPEAKER_COUNT,
                MAX_ATTEMPTS
        );

        assertThat(context.shouldCallAi()).isFalse();
        verify(stageSummaryRepository, never()).save(any());
        verify(stageSummaryRepository, never()).saveAndFlush(any());
        verify(speechRepository, never()).findStageSummarySourceSpeeches(any(), any());
    }

    @Test
    void prepareGeneration_createsPendingSummaryAndLoadsSourceSpeeches_whenEligible() {
        Room room = room(
                1L,
                LocalDateTime.of(2026, 6, 26, 10, 0),
                LocalDateTime.of(2026, 6, 26, 12, 0)
        );
        LocalDateTime midpoint = LocalDateTime.of(2026, 6, 26, 11, 0);
        Speech sourceSpeech = Speech.createMainOpinion(1L, 7L, "중간까지 나온 의견", SpeechStance.PRO);
        given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));
        given(speakingQueueRepository.countDistinctCompletedSpeakersByRoomId(1L))
                .willReturn(10L);
        given(stageSummaryRepository.findByRoomIdForUpdate(1L)).willReturn(Optional.empty());
        given(stageSummaryRepository.save(any(StageSummary.class))).willAnswer(invocation -> {
            StageSummary summary = invocation.getArgument(0);
            ReflectionTestUtils.setField(summary, "id", 77L);
            return summary;
        });
        given(speechRepository.findStageSummarySourceSpeeches(1L, midpoint))
                .willReturn(List.of(sourceSpeech));
        ArgumentCaptor<StageSummary> summaryCaptor = ArgumentCaptor.forClass(StageSummary.class);

        StageSummaryGenerationContext context = stageSummaryPersistenceService.prepareGeneration(
                1L,
                midpoint,
                MIN_COMPLETED_SPEAKER_COUNT,
                MAX_ATTEMPTS
        );

        verify(stageSummaryRepository).save(summaryCaptor.capture());
        verify(stageSummaryRepository, never()).saveAndFlush(any());
        StageSummary savedSummary = summaryCaptor.getValue();
        assertThat(savedSummary.getRoomId()).isEqualTo(1L);
        assertThat(savedSummary.getStatus()).isEqualTo(StageSummaryStatus.PENDING);
        assertThat(savedSummary.getTriggeredAt()).isEqualTo(midpoint);
        assertThat(savedSummary.getCompletedSpeakerCount()).isEqualTo(10);
        assertThat(savedSummary.getLastAttemptedAt()).isEqualTo(midpoint);
        assertThat(context.shouldCallAi()).isTrue();
        assertThat(context.summaryId()).isEqualTo(77L);
        assertThat(context.room()).isSameAs(room);
        assertThat(context.speeches()).containsExactly(sourceSpeech);
    }

    @Test
    void prepareGeneration_skipsExistingCompletedSummary() {
        Room room = room(
                1L,
                LocalDateTime.of(2026, 6, 26, 10, 0),
                LocalDateTime.of(2026, 6, 26, 12, 0)
        );
        StageSummary completed = StageSummary.pending(
                1L,
                LocalDateTime.of(2026, 6, 26, 11, 0),
                10
        );
        completed.complete(
                "완료된 중간 정리",
                List.of("쟁점 1", "쟁점 2", "쟁점 3"),
                12,
                LocalDateTime.of(2026, 6, 26, 11, 1)
        );
        given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));
        given(speakingQueueRepository.countDistinctCompletedSpeakersByRoomId(1L))
                .willReturn(10L);
        given(stageSummaryRepository.findByRoomIdForUpdate(1L)).willReturn(Optional.of(completed));

        StageSummaryGenerationContext context = stageSummaryPersistenceService.prepareGeneration(
                1L,
                LocalDateTime.of(2026, 6, 26, 11, 10),
                MIN_COMPLETED_SPEAKER_COUNT,
                MAX_ATTEMPTS
        );

        assertThat(context.shouldCallAi()).isFalse();
        verify(speechRepository, never()).findStageSummarySourceSpeeches(any(), any());
    }

    @Test
    void prepareGeneration_retriesFailedSummaryBelowMaxAttempts() {
        Room room = room(
                1L,
                LocalDateTime.of(2026, 6, 26, 10, 0),
                LocalDateTime.of(2026, 6, 26, 12, 0)
        );
        LocalDateTime retryAt = LocalDateTime.of(2026, 6, 26, 11, 10);
        StageSummary failed = failedSummary(1L, 77L, 1);
        Speech sourceSpeech = Speech.createMainOpinion(1L, 7L, "다시 요약할 의견", SpeechStance.CON);
        given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));
        given(speakingQueueRepository.countDistinctCompletedSpeakersByRoomId(1L))
                .willReturn(10L);
        given(stageSummaryRepository.findByRoomIdForUpdate(1L)).willReturn(Optional.of(failed));
        given(speechRepository.findStageSummarySourceSpeeches(1L, failed.getTriggeredAt()))
                .willReturn(List.of(sourceSpeech));

        StageSummaryGenerationContext context = stageSummaryPersistenceService.prepareGeneration(
                1L,
                retryAt,
                MIN_COMPLETED_SPEAKER_COUNT,
                MAX_ATTEMPTS
        );

        assertThat(failed.getStatus()).isEqualTo(StageSummaryStatus.PENDING);
        assertThat(failed.getLastAttemptedAt()).isEqualTo(retryAt);
        assertThat(failed.getRetryCount()).isEqualTo(1);
        assertThat(context.shouldCallAi()).isTrue();
        assertThat(context.summaryId()).isEqualTo(77L);
        assertThat(context.speeches()).containsExactly(sourceSpeech);
    }

    @Test
    void prepareGeneration_skipsFailedSummaryAtMaxAttempts() {
        Room room = room(
                1L,
                LocalDateTime.of(2026, 6, 26, 10, 0),
                LocalDateTime.of(2026, 6, 26, 12, 0)
        );
        StageSummary exhausted = failedSummary(1L, 77L, 3);
        given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));
        given(speakingQueueRepository.countDistinctCompletedSpeakersByRoomId(1L))
                .willReturn(10L);
        given(stageSummaryRepository.findByRoomIdForUpdate(1L)).willReturn(Optional.of(exhausted));

        StageSummaryGenerationContext context = stageSummaryPersistenceService.prepareGeneration(
                1L,
                LocalDateTime.of(2026, 6, 26, 11, 10),
                MIN_COMPLETED_SPEAKER_COUNT,
                MAX_ATTEMPTS
        );

        assertThat(context.shouldCallAi()).isFalse();
        assertThat(exhausted.getStatus()).isEqualTo(StageSummaryStatus.FAILED);
        verify(speechRepository, never()).findStageSummarySourceSpeeches(any(), any());
    }

    @Test
    void prepareGeneration_skipsRecentPendingSummary() {
        Room room = room(
                1L,
                LocalDateTime.of(2026, 6, 26, 10, 0),
                LocalDateTime.of(2026, 6, 26, 12, 0)
        );
        LocalDateTime now = LocalDateTime.of(2026, 6, 26, 11, 0, 5);
        StageSummary pending = StageSummary.pending(
                1L,
                LocalDateTime.of(2026, 6, 26, 11, 0),
                10
        );
        pending.markAttemptStarted(LocalDateTime.of(2026, 6, 26, 11, 0));
        given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));
        given(speakingQueueRepository.countDistinctCompletedSpeakersByRoomId(1L))
                .willReturn(10L);
        given(stageSummaryRepository.findByRoomIdForUpdate(1L)).willReturn(Optional.of(pending));

        StageSummaryGenerationContext context = stageSummaryPersistenceService.prepareGeneration(
                1L,
                now,
                MIN_COMPLETED_SPEAKER_COUNT,
                MAX_ATTEMPTS,
                GENERATE_TIMEOUT
        );

        assertThat(context.shouldCallAi()).isFalse();
        assertThat(pending.getStatus()).isEqualTo(StageSummaryStatus.PENDING);
        assertThat(pending.getRetryCount()).isZero();
        verify(speechRepository, never()).findStageSummarySourceSpeeches(any(), any());
    }

    @Test
    void prepareGeneration_retriesStalePendingSummary() {
        Room room = room(
                1L,
                LocalDateTime.of(2026, 6, 26, 10, 0),
                LocalDateTime.of(2026, 6, 26, 12, 0)
        );
        LocalDateTime now = LocalDateTime.of(2026, 6, 26, 11, 1);
        StageSummary stalePending = StageSummary.pending(
                1L,
                LocalDateTime.of(2026, 6, 26, 11, 0),
                10
        );
        ReflectionTestUtils.setField(stalePending, "id", 77L);
        stalePending.markAttemptStarted(LocalDateTime.of(2026, 6, 26, 11, 0));
        Speech sourceSpeech = Speech.createMainOpinion(1L, 7L, "stale pending source", SpeechStance.PRO);
        given(roomRepository.findByIdForUpdate(1L)).willReturn(Optional.of(room));
        given(speakingQueueRepository.countDistinctCompletedSpeakersByRoomId(1L))
                .willReturn(10L);
        given(stageSummaryRepository.findByRoomIdForUpdate(1L)).willReturn(Optional.of(stalePending));
        given(speechRepository.findStageSummarySourceSpeeches(1L, stalePending.getTriggeredAt()))
                .willReturn(List.of(sourceSpeech));

        StageSummaryGenerationContext context = stageSummaryPersistenceService.prepareGeneration(
                1L,
                now,
                MIN_COMPLETED_SPEAKER_COUNT,
                MAX_ATTEMPTS,
                GENERATE_TIMEOUT
        );

        assertThat(stalePending.getStatus()).isEqualTo(StageSummaryStatus.PENDING);
        assertThat(stalePending.getRetryCount()).isEqualTo(1);
        assertThat(stalePending.getLastAttemptedAt()).isEqualTo(now);
        assertThat(context.shouldCallAi()).isTrue();
        assertThat(context.summaryId()).isEqualTo(77L);
        assertThat(context.speeches()).containsExactly(sourceSpeech);
    }

    @Test
    void complete_savesModeratorSummaryAndKeyPoints() {
        StageSummary summary = StageSummary.pending(
                1L,
                LocalDateTime.of(2026, 6, 26, 11, 0),
                10
        );
        ReflectionTestUtils.setField(summary, "id", 77L);
        given(stageSummaryRepository.findById(77L)).willReturn(Optional.of(summary));
        StageSummaryResult result = new StageSummaryResult(
                "사회자 중간 정리",
                List.of("쟁점 1", "쟁점 2", "쟁점 3")
        );
        LocalDateTime completedAt = LocalDateTime.of(2026, 6, 26, 11, 1);

        StageSummary completed = stageSummaryPersistenceService.complete(77L, result, 12, completedAt);

        assertThat(completed.getStatus()).isEqualTo(StageSummaryStatus.COMPLETED);
        assertThat(completed.getModeratorSummary()).isEqualTo("사회자 중간 정리");
        assertThat(completed.getKeyPoints()).containsExactly("쟁점 1", "쟁점 2", "쟁점 3");
        assertThat(completed.getSpeechCount()).isEqualTo(12);
        assertThat(completed.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void fail_incrementsRetryCountAndStoresFailure() {
        StageSummary summary = StageSummary.pending(
                1L,
                LocalDateTime.of(2026, 6, 26, 11, 0),
                10
        );
        ReflectionTestUtils.setField(summary, "id", 77L);
        given(stageSummaryRepository.findById(77L)).willReturn(Optional.of(summary));

        StageSummary failed = stageSummaryPersistenceService.fail(77L, "AI timeout");

        assertThat(failed.getStatus()).isEqualTo(StageSummaryStatus.FAILED);
        assertThat(failed.getRetryCount()).isEqualTo(1);
        assertThat(failed.getErrorMessage()).isEqualTo("AI timeout");
    }

    private Room room(Long roomId, LocalDateTime startedAt, LocalDateTime endedAt) {
        Room room = Room.open(1L, "토론방 제목", startedAt, endedAt, 100);
        ReflectionTestUtils.setField(room, "id", roomId);
        return room;
    }

    private StageSummary failedSummary(Long roomId, Long summaryId, int retryCount) {
        StageSummary summary = StageSummary.pending(
                roomId,
                LocalDateTime.of(2026, 6, 26, 11, 0),
                10
        );
        ReflectionTestUtils.setField(summary, "id", summaryId);
        for (int attempt = 0; attempt < retryCount; attempt++) {
            summary.fail("failure " + attempt);
            if (attempt + 1 < retryCount) {
                summary.markPendingForRetry();
            }
        }
        return summary;
    }
}
