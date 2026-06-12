package com.sisibibi.api.domain.speech.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SpeakingQueueTest {

    @Test
    void waiting_createsWaitingRequest() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 6, 12, 11, 30);

        SpeakingQueue speakingQueue = SpeakingQueue.waiting(1L, 7L, 3, requestedAt);

        assertThat(speakingQueue.getRoomId()).isEqualTo(1L);
        assertThat(speakingQueue.getUserId()).isEqualTo(7L);
        assertThat(speakingQueue.getQueueOrder()).isEqualTo(3);
        assertThat(speakingQueue.getStatus()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(speakingQueue.getRequestedAt()).isEqualTo(requestedAt);
        assertThat(speakingQueue.getCanceledAt()).isNull();
    }

    @Test
    void cancel_changesWaitingRequestToCanceled() {
        SpeakingQueue speakingQueue = SpeakingQueue.waiting(
                1L,
                7L,
                3,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        LocalDateTime canceledAt = LocalDateTime.of(2026, 6, 12, 11, 35);

        speakingQueue.cancel(canceledAt);

        assertThat(speakingQueue.getStatus()).isEqualTo(SpeakingQueueStatus.CANCELED);
        assertThat(speakingQueue.getCanceledAt()).isEqualTo(canceledAt);
    }
}
