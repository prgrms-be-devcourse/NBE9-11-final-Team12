package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.entity.AiCounterIssue;
import com.sisibibi.api.domain.speech.entity.AiCounterIssueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.AiCounterIssueRepository;
import com.sisibibi.api.global.config.JpaAuditingConfig;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({
        JpaAuditingConfig.class,
        AiCounterIssuePersistenceService.class
})
class AiCounterIssuePersistenceServiceTest {

    @Autowired
    private AiCounterIssuePersistenceService aiCounterIssuePersistenceService;

    @Autowired
    private AiCounterIssueRepository aiCounterIssueRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    void createPendingIfAbsent_savesPendingIssue_whenTriggerDoesNotExist() {
        Optional<AiCounterIssue> result =
                aiCounterIssuePersistenceService.createPendingIfAbsent(
                        1L,
                        30L,
                        SpeechStance.CON
                );

        assertThat(result).isPresent();
        AiCounterIssue saved = result.get();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRoomId()).isEqualTo(1L);
        assertThat(saved.getTriggerQueueId()).isEqualTo(30L);
        assertThat(saved.getTargetStance()).isEqualTo(SpeechStance.CON);
        assertThat(saved.getStatus()).isEqualTo(AiCounterIssueStatus.PENDING);
        assertThat(saved.getRetryCount()).isZero();
        assertThat(saved.getLastAttemptedAt()).isNull();
    }

    @Test
    void createPendingIfAbsent_returnsEmpty_whenTriggerAlreadyExists() {
        aiCounterIssueRepository.saveAndFlush(
                AiCounterIssue.pending(1L, 30L, SpeechStance.CON)
        );

        Optional<AiCounterIssue> result =
                aiCounterIssuePersistenceService.createPendingIfAbsent(
                        1L,
                        30L,
                        SpeechStance.CON
                );

        assertThat(result).isEmpty();
        assertThat(aiCounterIssueRepository.findAll()).hasSize(1);
    }

    @Test
    void complete_marksIssueCompletedAndReturnsUpdatedIssue() {
        AiCounterIssue pending = aiCounterIssueRepository.saveAndFlush(
                AiCounterIssue.pending(1L, 30L, SpeechStance.CON)
        );

        AiCounterIssue completed = aiCounterIssuePersistenceService.complete(
                pending.getId(),
                "반대 측에서 검토할 핵심 쟁점입니다."
        );

        assertThat(completed.getStatus()).isEqualTo(AiCounterIssueStatus.COMPLETED);
        assertThat(completed.getContent()).isEqualTo("반대 측에서 검토할 핵심 쟁점입니다.");
        assertThat(completed.getCompletedAt()).isNotNull();
        assertThat(completed.getErrorMessage()).isNull();
    }

    @Test
    void fail_marksIssueFailedWithFallbackMessage_whenErrorMessageIsNull() {
        AiCounterIssue pending = aiCounterIssueRepository.saveAndFlush(
                AiCounterIssue.pending(1L, 30L, SpeechStance.CON)
        );

        aiCounterIssuePersistenceService.fail(pending.getId(), null);

        AiCounterIssue failed = aiCounterIssueRepository.findById(pending.getId())
                .orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(AiCounterIssueStatus.FAILED);
        assertThat(failed.getErrorMessage())
                .isEqualTo("AI counter issue generation failed.");
    }

    @Test
    void markAttemptStarted_increasesRetryCountAndRecordsAttemptTime() {
        AiCounterIssue pending = aiCounterIssueRepository.saveAndFlush(
                AiCounterIssue.pending(1L, 30L, SpeechStance.CON)
        );

        AiCounterIssue attempted =
                aiCounterIssuePersistenceService.markAttemptStarted(pending.getId());

        assertThat(attempted.getRetryCount()).isEqualTo(1);
        assertThat(attempted.getLastAttemptedAt()).isNotNull();
    }

    @Test
    void findRecentCompleted_returnsRecentCompletedIssues() {
        Room room = roomRepository.saveAndFlush(Room.open(
                1L,
                "AI 규제 토론",
                LocalDateTime.of(2026, 6, 25, 10, 0),
                LocalDateTime.of(2026, 6, 25, 12, 0),
                100
        ));
        AiCounterIssue first = aiCounterIssueRepository.saveAndFlush(
                AiCounterIssue.pending(room.getId(), 30L, SpeechStance.CON)
        );
        first.complete("첫 번째 쟁점", LocalDateTime.of(2026, 6, 25, 10, 10));
        AiCounterIssue failed = aiCounterIssueRepository.saveAndFlush(
                AiCounterIssue.pending(room.getId(), 31L, SpeechStance.PRO)
        );
        failed.fail("failed");
        AiCounterIssue second = aiCounterIssueRepository.saveAndFlush(
                AiCounterIssue.pending(room.getId(), 32L, SpeechStance.CON)
        );
        second.complete("두 번째 쟁점", LocalDateTime.of(2026, 6, 25, 10, 20));
        aiCounterIssueRepository.flush();

        List<AiCounterIssue> result =
                aiCounterIssuePersistenceService.findRecentCompleted(room.getId());

        assertThat(result)
                .extracting(AiCounterIssue::getContent)
                .containsExactly("두 번째 쟁점", "첫 번째 쟁점");
    }

    @Test
    void findRecentCompleted_throwsRoomNotFound_whenRoomDoesNotExist() {
        assertThatThrownBy(() -> aiCounterIssuePersistenceService.findRecentCompleted(999L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }
}
