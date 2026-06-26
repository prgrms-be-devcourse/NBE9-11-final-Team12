package com.sisibibi.api.domain.speech.util;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SpeakingStreakPolicyTest {

    private final SpeakingStreakPolicy policy = new SpeakingStreakPolicy();

    @Test
    void counterStanceFor_returnsCon_whenLatestThreeAssignmentsArePro() {
        Optional<SpeechStance> result = policy.counterStanceFor(List.of(
                assignedQueue(SpeechStance.PRO),
                assignedQueue(SpeechStance.PRO),
                assignedQueue(SpeechStance.PRO)
        ));

        assertThat(result).contains(SpeechStance.CON);
    }

    @Test
    void counterStanceFor_returnsPro_whenLatestThreeAssignmentsAreCon() {
        Optional<SpeechStance> result = policy.counterStanceFor(List.of(
                assignedQueue(SpeechStance.CON),
                assignedQueue(SpeechStance.CON),
                assignedQueue(SpeechStance.CON)
        ));

        assertThat(result).contains(SpeechStance.PRO);
    }

    @Test
    void counterStanceFor_returnsEmpty_whenAssignmentsAreLessThanThree() {
        Optional<SpeechStance> result = policy.counterStanceFor(List.of(
                assignedQueue(SpeechStance.PRO),
                assignedQueue(SpeechStance.PRO)
        ));

        assertThat(result).isEmpty();
    }

    @Test
    void counterStanceFor_returnsEmpty_whenLatestThreeAssignmentsAreMixed() {
        Optional<SpeechStance> result = policy.counterStanceFor(List.of(
                assignedQueue(SpeechStance.PRO),
                assignedQueue(SpeechStance.CON),
                assignedQueue(SpeechStance.PRO)
        ));

        assertThat(result).isEmpty();
    }

    @Test
    void counterStanceFor_returnsEmpty_whenLatestAssignmentHasNullStance() {
        SpeakingQueue nullStance = assignedQueue(SpeechStance.PRO);
        ReflectionTestUtils.setField(nullStance, "stance", null);

        Optional<SpeechStance> result = policy.counterStanceFor(List.of(
                nullStance,
                assignedQueue(SpeechStance.PRO),
                assignedQueue(SpeechStance.PRO)
        ));

        assertThat(result).isEmpty();
    }

    private SpeakingQueue assignedQueue(SpeechStance stance) {
        SpeakingQueue queue = SpeakingQueue.waiting(
                1L,
                10L,
                1,
                stance,
                LocalDateTime.of(2026, 6, 25, 10, 0)
        );
        queue.assign(
                LocalDateTime.of(2026, 6, 25, 10, 1),
                LocalDateTime.of(2026, 6, 25, 10, 4)
        );
        return queue;
    }
}
