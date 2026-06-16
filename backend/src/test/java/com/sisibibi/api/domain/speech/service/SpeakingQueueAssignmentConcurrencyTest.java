package com.sisibibi.api.domain.speech.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import java.time.LocalDateTime;
import java.util.Optional;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ActiveProfiles("test")
@Import(SpeakingQueuePersistenceService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpeakingQueueAssignmentConcurrencyTest {

    private static final LocalDateTime ASSIGNED_AT =
            LocalDateTime.of(2026, 6, 15, 10, 2);
    private static final LocalDateTime EXPIRES_AT =
            LocalDateTime.of(2026, 6, 15, 10, 4);

    @Autowired
    private SpeakingQueuePersistenceService speakingQueuePersistenceService;

    @Autowired
    private SpeakingQueueRepository speakingQueueRepository;

    @Autowired
    private RoomRepository roomRepository;

    private Long roomId;

    @BeforeEach
    void setUpWaitingQueue() {
        speakingQueueRepository.deleteAll();
        roomRepository.deleteAll();
        Room room = Room.open(1L, "토론방");
        ReflectionTestUtils.setField(room, "createdAt", LocalDateTime.now());
        roomId = roomRepository.saveAndFlush(room).getId();
        speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        roomId,
                        10L,
                        1,
                        LocalDateTime.of(2026, 6, 15, 10, 0)
                )
        );
        speakingQueueRepository.saveAndFlush(
                SpeakingQueue.waiting(
                        roomId,
                        20L,
                        2,
                        LocalDateTime.of(2026, 6, 15, 10, 1)
                )
        );
    }

    @Test
    void assignNextSpeaker_assignsOnlyOneSpeakerUnderConcurrentCalls()
            throws Exception {
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Optional<SpeakingQueue>> first =
                    executor.submit(() -> assignAfterSignal(start));
            Future<Optional<SpeakingQueue>> second =
                    executor.submit(() -> assignAfterSignal(start));

            start.countDown();

            Optional<SpeakingQueue> firstResult = first.get(5, TimeUnit.SECONDS);
            Optional<SpeakingQueue> secondResult = second.get(5, TimeUnit.SECONDS);

            long assignedResultCount =
                    java.util.stream.Stream.of(firstResult, secondResult)
                            .filter(Optional::isPresent)
                            .count();
            long assignedRowCount = speakingQueueRepository.findAll().stream()
                    .filter(queue -> queue.getStatus() == SpeakingQueueStatus.ASSIGNED)
                    .count();

            assertThat(assignedResultCount).isEqualTo(1);
            assertThat(assignedRowCount).isEqualTo(1);
        }
    }

    private Optional<SpeakingQueue> assignAfterSignal(CountDownLatch start)
            throws InterruptedException {
        start.await();
        return speakingQueuePersistenceService.assignNextSpeaker(
                roomId,
                ASSIGNED_AT,
                EXPIRES_AT
        );
    }
}
