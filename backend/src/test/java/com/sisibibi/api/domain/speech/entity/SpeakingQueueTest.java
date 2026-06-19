package com.sisibibi.api.domain.speech.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SpeakingQueueTest {

    @Test
    void waiting_createsWaitingRequest() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 6, 12, 11, 30);

        SpeakingQueue speakingQueue = SpeakingQueue.waiting(
                1L,
                7L,
                SpeechStance.PRO,
                requestedAt
        );

        assertThat(speakingQueue.getRoomId()).isEqualTo(1L);
        assertThat(speakingQueue.getUserId()).isEqualTo(7L);
        assertThat(speakingQueue.getQueueOrder()).isNull();
        assertThat(speakingQueue.getStatus()).isEqualTo(SpeakingQueueStatus.WAITING);
        assertThat(speakingQueue.getRequestedAt()).isEqualTo(requestedAt);
        assertThat(speakingQueue.getCanceledAt()).isNull();
        assertThat(speakingQueue.getActiveRequest()).isTrue();
    }

    @Test
    void waiting_allowsNeutralRequestWhenStanceIsNull() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 6, 12, 11, 30);

        SpeakingQueue speakingQueue = SpeakingQueue.waiting(
                1L,
                7L,
                null,
                requestedAt
        );

        assertThat(speakingQueue.getStance()).isNull();
        assertThat(speakingQueue.getStatus()).isEqualTo(SpeakingQueueStatus.WAITING);
    }

    @Test
    void waitingWithQueueOrder_createsWaitingRequestWithRoomScopedOrder() {
        SpeakingQueue speakingQueue = SpeakingQueue.waiting(
                1L,
                7L,
                3,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );

        assertThat(speakingQueue.getQueueOrder()).isEqualTo(3);
    }

    @Test
    void cancel_changesWaitingRequestToCanceled() {
        SpeakingQueue speakingQueue = SpeakingQueue.waiting(
                1L,
                7L,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        LocalDateTime canceledAt = LocalDateTime.of(2026, 6, 12, 11, 35);

        speakingQueue.cancel(canceledAt);

        assertThat(speakingQueue.getStatus()).isEqualTo(SpeakingQueueStatus.CANCELED);
        assertThat(speakingQueue.getCanceledAt()).isEqualTo(canceledAt);
        assertThat(speakingQueue.getActiveRequest()).isNull();
    }

    @Test
    void cancel_rejectsAssignedRequest() {
        SpeakingQueue speakingQueue = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        speakingQueue.assign(
                LocalDateTime.of(2026, 6, 12, 11, 31),
                LocalDateTime.of(2026, 6, 12, 11, 33)
        );

        assertThatThrownBy(() ->
                speakingQueue.cancel(LocalDateTime.of(2026, 6, 12, 11, 35)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only waiting speaking requests can be canceled.");
    }

    @Test
    void assign_changesWaitingRequestToAssigned() {
        SpeakingQueue speakingQueue = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        LocalDateTime assignedAt = LocalDateTime.of(2026, 6, 12, 11, 31);
        LocalDateTime expiresAt = LocalDateTime.of(2026, 6, 12, 11, 33);

        speakingQueue.assign(assignedAt, expiresAt);

        assertThat(speakingQueue.getStatus()).isEqualTo(SpeakingQueueStatus.ASSIGNED);
        assertThat(speakingQueue.getActiveRequest()).isTrue();
        assertThat(speakingQueue.getAssignedAt()).isEqualTo(assignedAt);
        assertThat(speakingQueue.getExpiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void assign_rejectsNonWaitingRequest() {
        SpeakingQueue speakingQueue = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        speakingQueue.cancel(LocalDateTime.of(2026, 6, 12, 11, 35));

        assertThatThrownBy(() -> speakingQueue.assign(
                LocalDateTime.of(2026, 6, 12, 11, 36),
                LocalDateTime.of(2026, 6, 12, 11, 38)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only waiting speaking requests can be assigned.");
    }

    @Test
    void assign_rejectsExpirationThatIsNotAfterAssignment() {
        SpeakingQueue speakingQueue = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        LocalDateTime assignedAt = LocalDateTime.of(2026, 6, 12, 11, 31);

        assertThatThrownBy(() -> speakingQueue.assign(assignedAt, assignedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Speaking expiration must be after assignment.");
    }

    @Test
    void complete_changesAssignedRequestToCompleted() {
        SpeakingQueue speakingQueue = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        speakingQueue.assign(
                LocalDateTime.of(2026, 6, 12, 11, 31),
                LocalDateTime.of(2026, 6, 12, 11, 33)
        );

        speakingQueue.complete();

        assertThat(speakingQueue.getStatus()).isEqualTo(SpeakingQueueStatus.COMPLETED);
        assertThat(speakingQueue.getActiveRequest()).isNull();
    }

    @Test
    void complete_rejectsWaitingRequest() {
        SpeakingQueue speakingQueue = SpeakingQueue.waiting(
                1L,
                7L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );

        assertThatThrownBy(speakingQueue::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only assigned speaking requests can be completed.");
    }
}
