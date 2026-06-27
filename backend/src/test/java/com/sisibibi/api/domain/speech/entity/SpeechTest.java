package com.sisibibi.api.domain.speech.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SpeechTest {

    @Test
    void createMainOpinion_createsSpeakingSpeech() {
        Speech speech = Speech.createMainOpinion(1L, 2L, "의견", SpeechStance.PRO);

        assertThat(speech.getStatus()).isEqualTo(SpeechStatus.SPEAKING);
        assertThat(speech.getStartedAt()).isNull();
    }

    @Test
    void createMainOpinion_setsStartedAt_whenStartedAtIsProvided() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 6, 12, 12, 0);

        Speech speech = Speech.createMainOpinion(
                1L,
                2L,
                "의견",
                SpeechStance.PRO,
                startedAt
        );

        assertThat(speech.getStatus()).isEqualTo(SpeechStatus.SPEAKING);
        assertThat(speech.getStartedAt()).isEqualTo(startedAt);
    }

    @Test
    void softDelete_preservesFirstDeletedAt_whenCalledRepeatedly() {
        Speech speech = Speech.createMainOpinion(1L, 2L, "의견", SpeechStance.PRO);
        LocalDateTime firstDeletedAt = LocalDateTime.of(2026, 6, 19, 10, 0);
        LocalDateTime secondDeletedAt = firstDeletedAt.plusMinutes(1);

        speech.softDelete(firstDeletedAt);
        speech.softDelete(secondDeletedAt);

        assertThat(speech.isDeleted()).isTrue();
        assertThat(speech.getDeletedAt()).isEqualTo(firstDeletedAt);
    }
}
