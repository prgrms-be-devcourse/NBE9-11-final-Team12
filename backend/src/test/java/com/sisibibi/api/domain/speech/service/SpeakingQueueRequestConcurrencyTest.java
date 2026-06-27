package com.sisibibi.api.domain.speech.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipant;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.entity.RoomQueueSequence;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.RoomQueueSequenceRepository;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.util.SpeakingStreakPolicy;
import com.sisibibi.api.domain.usersanction.service.UserSanctionPolicyService;
import java.time.LocalDateTime;
import java.util.List;
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
@Import({
        SpeakingQueuePersistenceService.class,
        SpeakingStreakPolicy.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpeakingQueueRequestConcurrencyTest {

    private static final int REQUEST_COUNT = 20;

    @Autowired
    private SpeakingQueuePersistenceService speakingQueuePersistenceService;

    @Autowired
    private SpeakingQueueRepository speakingQueueRepository;

    @Autowired
    private RoomQueueSequenceRepository roomQueueSequenceRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomParticipantRepository roomParticipantRepository;

    @MockitoBean
    private UserSanctionPolicyService userSanctionPolicyService;

    private Long roomId;

    @BeforeEach
    void setUpRoom() {
        speakingQueueRepository.deleteAll();
        roomQueueSequenceRepository.deleteAll();
        roomParticipantRepository.deleteAll();
        roomRepository.deleteAll();

        LocalDateTime startedAt = LocalDateTime.of(2026, 6, 15, 10, 0);
        LocalDateTime endedAt = LocalDateTime.of(2099, 6, 15, 12, 0);
        Room room = Room.open(1L, "토론방", startedAt, endedAt, 100);
        ReflectionTestUtils.setField(room, "createdAt", LocalDateTime.now());
        roomId = roomRepository.saveAndFlush(room).getId();
        roomQueueSequenceRepository.saveAndFlush(RoomQueueSequence.create(roomId, startedAt));

        for (int index = 0; index < REQUEST_COUNT; index++) {
            roomParticipantRepository.saveAndFlush(
                    RoomParticipant.join(roomId, userId(index))
            );
        }
    }

    @Test
    void createWaitingRequest_assignsUniqueQueueOrderUnderConcurrentRequests()
            throws Exception {
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT)) {
            List<Future<SpeakingQueue>> futures = java.util.stream.IntStream
                    .range(0, REQUEST_COUNT)
                    .mapToObj(index -> executor.submit(() -> requestAfterSignal(start, index)))
                    .toList();

            start.countDown();

            for (Future<SpeakingQueue> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        }

        List<Integer> queueOrders = speakingQueueRepository.findAll()
                .stream()
                .map(SpeakingQueue::getQueueOrder)
                .sorted()
                .toList();

        assertThat(queueOrders).hasSize(REQUEST_COUNT);
        assertThat(queueOrders).doesNotHaveDuplicates();
        assertThat(queueOrders).containsExactlyElementsOf(
                java.util.stream.IntStream
                        .rangeClosed(1, REQUEST_COUNT)
                        .boxed()
                        .toList()
        );
        assertThat(roomQueueSequenceRepository.findById(roomId).orElseThrow()
                .getNextQueueOrder()).isEqualTo(REQUEST_COUNT + 1);
    }

    private SpeakingQueue requestAfterSignal(CountDownLatch start, int index)
            throws InterruptedException {
        start.await();
        return speakingQueuePersistenceService.createWaitingRequest(
                roomId,
                userId(index),
                index % 2 == 0 ? SpeechStance.PRO : SpeechStance.CON
        );
    }

    private Long userId(int index) {
        return 1000L + index;
    }
}
