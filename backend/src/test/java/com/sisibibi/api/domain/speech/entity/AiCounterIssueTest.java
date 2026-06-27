package com.sisibibi.api.domain.speech.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AiCounterIssueTest {

    @Test
    void complete_throwsException_whenIssueIsAlreadyCompleted() {
        AiCounterIssue issue = AiCounterIssue.pending(1L, 30L, SpeechStance.CON);
        issue.complete("content", LocalDateTime.of(2026, 6, 26, 10, 0));

        assertThatThrownBy(() ->
                issue.complete("other content", LocalDateTime.of(2026, 6, 26, 10, 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only PENDING AI counter issues can complete");
    }

    @Test
    void fail_throwsException_whenIssueIsAlreadyCompleted() {
        AiCounterIssue issue = AiCounterIssue.pending(1L, 30L, SpeechStance.CON);
        issue.complete("content", LocalDateTime.of(2026, 6, 26, 10, 0));

        assertThatThrownBy(() -> issue.fail("failed"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only PENDING AI counter issues can fail");
    }

    @Test
    void markAttemptStarted_throwsException_whenIssueIsAlreadyFailed() {
        AiCounterIssue issue = AiCounterIssue.pending(1L, 30L, SpeechStance.CON);
        issue.fail("failed");

        assertThatThrownBy(() ->
                issue.markAttemptStarted(LocalDateTime.of(2026, 6, 26, 10, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only PENDING AI counter issues can start an attempt");
    }
}
