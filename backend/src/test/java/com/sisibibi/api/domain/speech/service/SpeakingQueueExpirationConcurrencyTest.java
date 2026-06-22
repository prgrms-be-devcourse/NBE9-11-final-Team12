package com.sisibibi.api.domain.speech.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.usersanction.service.UserSanctionPolicyService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ActiveProfiles("test")
@Import(SpeakingQueuePersistenceService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpeakingQueueExpirationConcurrencyTest {

    private static final Long USER_ID = 10L;
    private static final LocalDateTime EXPIRED_AT =
            LocalDateTime.of(2026, 6, 15, 10, 4);
    private static final LocalDateTime NOW =
            LocalDateTime.of(2026, 6, 15, 10, 5);

    @Autowired
    private SpeakingQueuePersistenceService speakingQueuePersistenceService;

    @Autowired
    private SpeakingQueueRepository speakingQueueRepository;

    @Autowired
    private RoomRepository roomRepository;

    @MockitoBean
    private UserSanctionPolicyService userSanctionPolicyService;

    private Long roomId;

    @BeforeEach
    void setUpAssignedSpeaker() {
        speakingQueueRepository.deleteAll();
        roomRepository.deleteAll();

        LocalDateTime firstStartedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        LocalDateTime firstEndedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
        Room room = Room.open(1L, "토론방", firstStartedAt, firstEndedAt, 100);
        ReflectionTestUtils.setField(room, "createdAt", LocalDateTime.now());
        roomId = roomRepository.saveAndFlush(room).getId();

        SpeakingQueue assigned = SpeakingQueue.waiting(
                roomId,
                USER_ID,
                1,
                SpeechStance.PRO,
                LocalDateTime.of(2026, 6, 15, 10, 0)
        );
        assigned.assign(
                LocalDateTime.of(2026, 6, 15, 10, 2),
                EXPIRED_AT
        );
        speakingQueueRepository.saveAndFlush(assigned);
    }

    @Test
    void manualCompletionAndExpiration_completeSpeakerOnlyOnce()
            throws Exception {
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> manualCompletion =
                    executor.submit(() -> completeManuallyAfterSignal(start));
            Future<Boolean> automaticExpiration =
                    executor.submit(() -> expireAfterSignal(start));

            start.countDown();

            long successfulTransitions =
                    java.util.stream.Stream.of(
                                    manualCompletion.get(5, TimeUnit.SECONDS),
                                    automaticExpiration.get(5, TimeUnit.SECONDS)
                            )
                            .filter(Boolean::booleanValue)
                            .count();

            long completedCount = speakingQueueRepository.findAll().stream()
                    .filter(queue ->
                            queue.getStatus() == SpeakingQueueStatus.COMPLETED)
                    .count();
            long assignedCount = speakingQueueRepository.findAll().stream()
                    .filter(queue ->
                            queue.getStatus() == SpeakingQueueStatus.ASSIGNED)
                    .count();

            assertThat(successfulTransitions).isEqualTo(1);
            assertThat(completedCount).isEqualTo(1);
            assertThat(assignedCount).isZero();
        }
    }

    private boolean completeManuallyAfterSignal(CountDownLatch start)
            throws InterruptedException {
        start.await();
        try {
            speakingQueuePersistenceService.completeCurrentSpeaker(
                    roomId,
                    USER_ID
            );
            return true;
        } catch (CustomException exception) {
            if (exception.getErrorCode() == ErrorCode.CURRENT_SPEAKER_NOT_FOUND) {
                return false;
            }
            throw exception;
        }
    }

    private boolean expireAfterSignal(CountDownLatch start)
            throws InterruptedException {
        start.await();
        return speakingQueuePersistenceService
                .expireCurrentSpeaker(roomId, NOW)
                .isPresent();
    }
}
