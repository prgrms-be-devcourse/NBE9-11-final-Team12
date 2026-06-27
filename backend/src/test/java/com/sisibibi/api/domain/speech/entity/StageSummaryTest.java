package com.sisibibi.api.domain.speech.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class StageSummaryTest {

    @Test
    void fail_preservesErrorMessageUpToTwoThousandCharacters() {
        StageSummary summary = StageSummary.pending(
                1L,
                LocalDateTime.of(2026, 6, 26, 11, 0),
                10
        );
        String errorMessage = "x".repeat(2_000);

        summary.fail(errorMessage);

        assertThat(summary.getErrorMessage()).hasSize(2_000);
    }
}
