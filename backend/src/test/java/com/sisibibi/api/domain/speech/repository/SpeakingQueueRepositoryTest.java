package com.sisibibi.api.domain.speech.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class SpeakingQueueRepositoryTest {

    @Autowired
    private SpeakingQueueRepository speakingQueueRepository;

    @Test
    void save_persistsWaitingRequest() {
        SpeakingQueue request = SpeakingQueue.waiting(
                1L,
                7L,
                1,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        SpeakingQueue saved = speakingQueueRepository.saveAndFlush(request);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getQueueOrder()).isEqualTo(1);
        assertThat(speakingQueueRepository.existsByRoomIdAndUserIdAndStatusIn(
                1L,
                7L,
                List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED)
        )).isTrue();
    }

    @Test
    void findByRoomIdAndUserIdAndStatusIn_returnsActiveRequest() {
        SpeakingQueue saved = speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        1L,
                        10L,
                        1,
                        SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
                )
        );

        Optional<SpeakingQueue> found = speakingQueueRepository
                .findByRoomIdAndUserIdAndStatusIn(
                        1L,
                        10L,
                        List.of(
                                SpeakingQueueStatus.WAITING,
                                SpeakingQueueStatus.ASSIGNED
                        )
                );

        assertThat(found).contains(saved);
    }

    @Test
    void save_rejectsDuplicateActiveRequestInSameRoom() {
        speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        1L,
                        7L,
                        1,
                        SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
                )
        );

        assertThatThrownBy(() -> speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        1L,
                        7L,
                        2,
                        SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 31)
                )
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void save_rejectsDuplicateAssignedSpeakerInSameRoom() {
        SpeakingQueue firstAssigned = SpeakingQueue.waiting(
                1L,
                7L,
                1,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(firstAssigned);
        speakingQueueRepository.saveAndFlush(firstAssigned);

        SpeakingQueue secondAssigned = SpeakingQueue.waiting(
                1L,
                8L,
                2,
                SpeechStance.CON,
                LocalDateTime.of(2026, 6, 12, 11, 31)
        );
        assign(secondAssigned);

        assertThatThrownBy(() -> speakingQueueRepository.saveAndFlush(secondAssigned))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findFirstWaiting_returnsLowestQueueOrder() {
        speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        1L,
                        10L,
                        20,
                        SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 31)
                )
        );
        SpeakingQueue first = speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        1L,
                        20L,
                        15,
                        SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
                )
        );

        SpeakingQueue found = speakingQueueRepository
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING
                )
                .orElseThrow();

        assertThat(found.getId()).isEqualTo(first.getId());
        assertThat(found.getQueueOrder()).isEqualTo(15);
    }

    @Test
    void findWaitingPageForRedisReadFallback_returnsRequestedOffsetAndLimit() {
        speakingQueueRepository.saveAllAndFlush(List.of(
                SpeakingQueue.waiting(
                        1L,
                        10L,
                        10,
                        SpeechStance.PRO,
                        LocalDateTime.of(2026, 6, 12, 11, 30)
                ),
                SpeakingQueue.waiting(
                        1L,
                        20L,
                        20,
                        SpeechStance.PRO,
                        LocalDateTime.of(2026, 6, 12, 11, 31)
                ),
                SpeakingQueue.waiting(
                        1L,
                        30L,
                        30,
                        SpeechStance.PRO,
                        LocalDateTime.of(2026, 6, 12, 11, 32)
                ),
                SpeakingQueue.waiting(
                        1L,
                        40L,
                        40,
                        SpeechStance.PRO,
                        LocalDateTime.of(2026, 6, 12, 11, 33)
                )
        ));

        List<SpeakingQueue> waitingQueues =
                speakingQueueRepository.findWaitingPageForRedisReadFallback(
                        1L,
                        SpeakingQueueStatus.WAITING.name(),
                        1,
                        2
                );

        assertThat(waitingQueues)
                .extracting(SpeakingQueue::getQueueOrder)
                .containsExactly(20, 30);
    }

    @Test
    void countByRoomIdAndStatusAndQueueOrderLessThan_countsPreviousWaitingRequests() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                40L,
                5,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 29)
        );
        assign(assigned);
        speakingQueueRepository.saveAllAndFlush(List.of(
                assigned,
                SpeakingQueue.waiting(
                        1L,
                        10L,
                        10,
                        SpeechStance.PRO,
                        LocalDateTime.of(2026, 6, 12, 11, 30)
                ),
                SpeakingQueue.waiting(
                        1L,
                        20L,
                        20,
                        SpeechStance.PRO,
                        LocalDateTime.of(2026, 6, 12, 11, 31)
                ),
                SpeakingQueue.waiting(
                        1L,
                        30L,
                        30,
                        SpeechStance.PRO,
                        LocalDateTime.of(2026, 6, 12, 11, 32)
                )
        ));

        long previousWaitingCount =
                speakingQueueRepository.countByRoomIdAndStatusAndQueueOrderLessThan(
                        1L,
                        SpeakingQueueStatus.WAITING,
                        30
                );

        assertThat(previousWaitingCount).isEqualTo(2L);
    }

    @Test
    void findFirstWaitingByStance_returnsLowestQueueOrderInRequestedStance() {
        speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        1L,
                        10L,
                        10,
                        SpeechStance.PRO,
                        LocalDateTime.of(2026, 6, 12, 11, 30)
                )
        );
        SpeakingQueue firstCon = speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        1L,
                        20L,
                        20,
                        SpeechStance.CON,
                        LocalDateTime.of(2026, 6, 12, 11, 31)
                )
        );
        speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        1L,
                        30L,
                        30,
                        SpeechStance.CON,
                        LocalDateTime.of(2026, 6, 12, 11, 32)
                )
        );

        SpeakingQueue found = speakingQueueRepository
                .findFirstByRoomIdAndStatusAndStanceOrderByQueueOrderAsc(
                        1L,
                        SpeakingQueueStatus.WAITING,
                        SpeechStance.CON
                )
                .orElseThrow();

        assertThat(found.getId()).isEqualTo(firstCon.getId());
        assertThat(found.getQueueOrder()).isEqualTo(20);
        assertThat(found.getStance()).isEqualTo(SpeechStance.CON);
    }

    @Test
    void findRecentAssignments_returnsLatestThreeWithStance() {
        SpeakingQueue oldest = completedQueue(
                1L,
                10L,
                10,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 10)
        );
        SpeakingQueue third = completedQueue(
                1L,
                20L,
                20,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 20)
        );
        SpeakingQueue second = completedQueue(
                1L,
                30L,
                30,
                SpeechStance.CON,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        SpeakingQueue first = completedQueue(
                1L,
                40L,
                40,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 40)
        );
        speakingQueueRepository.saveAllAndFlush(List.of(oldest, third, second, first));
        speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        1L,
                        50L,
                        50,
                        SpeechStance.CON,
                        LocalDateTime.of(2026, 6, 12, 11, 50)
                )
        );

        List<SpeakingQueue> recent =
                speakingQueueRepository
                        .findTop3ByRoomIdAndStatusInAndStanceIsNotNullOrderByAssignedAtDesc(
                                1L,
                                List.of(SpeakingQueueStatus.COMPLETED)
                        );

        assertThat(recent)
                .extracting(SpeakingQueue::getUserId)
                .containsExactly(40L, 30L, 20L);
    }

    @Test
    void countDistinctCompletedSpeakersByRoomId_countsCompletedUsersOnce() {
        SpeakingQueue firstUserFirstTurn = completedQueue(
                1L,
                10L,
                10,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 10)
        );
        SpeakingQueue firstUserSecondTurn = completedQueue(
                1L,
                10L,
                20,
                SpeechStance.CON,
                LocalDateTime.of(2026, 6, 12, 11, 20)
        );
        SpeakingQueue secondUser = completedQueue(
                1L,
                20L,
                30,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        SpeakingQueue otherRoom = completedQueue(
                2L,
                30L,
                10,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 40)
        );
        SpeakingQueue waiting = SpeakingQueue.waiting(
                1L,
                40L,
                40,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 50)
        );
        speakingQueueRepository.saveAllAndFlush(List.of(
                firstUserFirstTurn,
                firstUserSecondTurn,
                secondUser,
                otherRoom,
                waiting
        ));

        long completedSpeakerCount =
                speakingQueueRepository.countDistinctCompletedSpeakersByRoomId(1L);

        assertThat(completedSpeakerCount).isEqualTo(2L);
    }

    @Test
    void findByRoomIdAndStatus_returnsCurrentAssignedSpeaker() {
        SpeakingQueue assigned = SpeakingQueue.waiting(
                1L,
                10L,
                15,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        assign(assigned);
        SpeakingQueue saved = speakingQueueRepository.saveAndFlush(assigned);

        Optional<SpeakingQueue> found = speakingQueueRepository
                .findByRoomIdAndStatus(1L, SpeakingQueueStatus.ASSIGNED);

        assertThat(found).contains(saved);
    }

    @Test
    void findRoomIdsRequiringAssignment_returnsOnlyRoomsWithoutAssignedSpeaker() {
        speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        1L,
                        10L,
                        1,
                        SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
                )
        );
        SpeakingQueue assigned = SpeakingQueue.waiting(
                2L,
                20L,
                2,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 31)
        );
        assign(assigned);
        speakingQueueRepository.saveAndFlush(assigned);
        speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        2L,
                        21L,
                        3,
                        SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 32)
                )
        );

        List<Long> candidateRoomIds =
                speakingQueueRepository.findRoomIdsRequiringAssignment();

        assertThat(candidateRoomIds).containsExactly(1L);
    }

    @Test
    void findRoomIdsWithExpiredSpeaker_returnsOnlyExpiredAssignedRooms() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 12, 11, 35);
        SpeakingQueue expired = SpeakingQueue.waiting(
                1L,
                10L,
                1,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        expired.assign(
                LocalDateTime.of(2026, 6, 12, 11, 31),
                LocalDateTime.of(2026, 6, 12, 11, 33)
        );
        speakingQueueRepository.saveAndFlush(expired);

        SpeakingQueue notExpired = SpeakingQueue.waiting(
                2L,
                20L,
                2,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 32)
        );
        notExpired.assign(
                LocalDateTime.of(2026, 6, 12, 11, 34),
                LocalDateTime.of(2026, 6, 12, 11, 36)
        );
        speakingQueueRepository.saveAndFlush(notExpired);

        speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        3L,
                        30L,
                        3,
                        SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
                )
        );

        List<Long> expiredRoomIds =
                speakingQueueRepository.findRoomIdsWithExpiredSpeaker(now);

        assertThat(expiredRoomIds).containsExactly(1L);
    }

    @Test
    void findRoomIdsRequiringIdleWarning_returnsOnlyIdleAssignedRoomsBeforeWarning() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 12, 11, 31, 20);
        SpeakingQueue warningRequired = SpeakingQueue.waiting(
                1L,
                10L,
                1,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        warningRequired.assign(
                LocalDateTime.of(2026, 6, 12, 11, 31),
                LocalDateTime.of(2026, 6, 12, 11, 34)
        );
        speakingQueueRepository.saveAndFlush(warningRequired);

        SpeakingQueue alreadyWarned = SpeakingQueue.waiting(
                2L,
                20L,
                1,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        alreadyWarned.assign(
                LocalDateTime.of(2026, 6, 12, 11, 31),
                LocalDateTime.of(2026, 6, 12, 11, 34)
        );
        alreadyWarned.markIdleWarningIfDue(
                now,
                Duration.ofSeconds(20),
                Duration.ofSeconds(40)
        );
        speakingQueueRepository.saveAndFlush(alreadyWarned);

        SpeakingQueue expiringSoon = SpeakingQueue.waiting(
                3L,
                30L,
                1,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        expiringSoon.assign(
                LocalDateTime.of(2026, 6, 12, 11, 31),
                LocalDateTime.of(2026, 6, 12, 11, 31, 50)
        );
        speakingQueueRepository.saveAndFlush(expiringSoon);

        List<Long> roomIds = speakingQueueRepository.findRoomIdsRequiringIdleWarning(
                now,
                Duration.ofSeconds(20),
                Duration.ofSeconds(40)
        );

        assertThat(roomIds).containsExactly(1L);
    }

    @Test
    void findRoomIdsWithIdleTimedOutSpeaker_returnsOnlyWarnedRoomsAfterTimeoutDelay() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 12, 11, 31, 40);
        SpeakingQueue timedOut = SpeakingQueue.waiting(
                1L,
                10L,
                1,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        timedOut.assign(
                LocalDateTime.of(2026, 6, 12, 11, 31),
                LocalDateTime.of(2026, 6, 12, 11, 34)
        );
        timedOut.markIdleWarningIfDue(
                LocalDateTime.of(2026, 6, 12, 11, 31, 20),
                Duration.ofSeconds(20),
                Duration.ofSeconds(40)
        );
        speakingQueueRepository.saveAndFlush(timedOut);

        SpeakingQueue notYetTimedOut = SpeakingQueue.waiting(
                2L,
                20L,
                1,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 12, 11, 30)
        );
        notYetTimedOut.assign(
                LocalDateTime.of(2026, 6, 12, 11, 31),
                LocalDateTime.of(2026, 6, 12, 11, 34)
        );
        notYetTimedOut.markIdleWarningIfDue(
                LocalDateTime.of(2026, 6, 12, 11, 31, 30),
                Duration.ofSeconds(20),
                Duration.ofSeconds(40)
        );
        speakingQueueRepository.saveAndFlush(notYetTimedOut);

        List<Long> roomIds = speakingQueueRepository.findRoomIdsWithIdleTimedOutSpeaker(
                now,
                Duration.ofSeconds(20)
        );

        assertThat(roomIds).containsExactly(1L);
    }

    private void assign(SpeakingQueue speakingQueue) {
        speakingQueue.assign(
                LocalDateTime.of(2026, 6, 12, 11, 31),
                LocalDateTime.of(2026, 6, 12, 11, 33)
        );
    }

    private SpeakingQueue completedQueue(
            Long roomId,
            Long userId,
            int queueOrder,
            SpeechStance stance,
            LocalDateTime assignedAt
    ) {
        SpeakingQueue speakingQueue = SpeakingQueue.waiting(
                roomId,
                userId,
                queueOrder,
                stance,
                assignedAt.minusMinutes(1)
        );
        speakingQueue.assign(assignedAt, assignedAt.plusMinutes(3));
        speakingQueue.complete();
        return speakingQueue;
    }
}
