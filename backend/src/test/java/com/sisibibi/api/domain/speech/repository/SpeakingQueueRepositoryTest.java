package com.sisibibi.api.domain.speech.repository;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SpeakingQueueRepositoryTest {

    @Autowired
    private SpeakingQueueRepository speakingQueueRepository;

    @Test
    void findDistinctRoomIdsByStatusAndAssignedAtLessThanEqual_returnsOnlyExpiredAssignedRooms() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresBefore = now.minusMinutes(5);

        speakingQueueRepository.save(assignedQueue(900_001L, 910_001L, 1, now.minusMinutes(10)));
        speakingQueueRepository.save(assignedQueue(900_002L, 910_002L, 1, now.minusMinutes(1)));
        speakingQueueRepository.save(waitingQueue(900_003L, 910_003L, 1));
        speakingQueueRepository.save(completedQueue(900_004L, 910_004L, 1, now.minusMinutes(10)));
        speakingQueueRepository.save(expiredQueue(900_005L, 910_005L, 1, now.minusMinutes(10)));
        speakingQueueRepository.save(canceledQueue(900_006L, 910_006L, 1));
        speakingQueueRepository.save(assignedQueue(900_001L, 910_007L, 2, now.minusMinutes(9)));

        List<Long> roomIds = speakingQueueRepository
                .findDistinctRoomIdsByStatusAndAssignedAtLessThanEqual(
                        SpeakingQueueStatus.ASSIGNED,
                        expiresBefore
                );

        assertThat(roomIds).containsExactly(900_001L);
    }

    private SpeakingQueue waitingQueue(Long roomId, Long userId, int queueOrder) {
        return SpeakingQueue.create(roomId, userId, queueOrder, LocalDateTime.now());
    }

    private SpeakingQueue assignedQueue(
            Long roomId,
            Long userId,
            int queueOrder,
            LocalDateTime assignedAt
    ) {
        SpeakingQueue speakingQueue = waitingQueue(roomId, userId, queueOrder);
        speakingQueue.assign(assignedAt);
        return speakingQueue;
    }

    private SpeakingQueue completedQueue(
            Long roomId,
            Long userId,
            int queueOrder,
            LocalDateTime assignedAt
    ) {
        SpeakingQueue speakingQueue = assignedQueue(roomId, userId, queueOrder, assignedAt);
        speakingQueue.complete(assignedAt.plusMinutes(1));
        return speakingQueue;
    }

    private SpeakingQueue expiredQueue(
            Long roomId,
            Long userId,
            int queueOrder,
            LocalDateTime assignedAt
    ) {
        SpeakingQueue speakingQueue = assignedQueue(roomId, userId, queueOrder, assignedAt);
        speakingQueue.expire(assignedAt.plusMinutes(5));
        return speakingQueue;
    }

    private SpeakingQueue canceledQueue(Long roomId, Long userId, int queueOrder) {
        SpeakingQueue speakingQueue = waitingQueue(roomId, userId, queueOrder);
        speakingQueue.cancel(LocalDateTime.now());
        return speakingQueue;
    }
}
