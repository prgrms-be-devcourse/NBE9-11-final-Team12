package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.repository.RedisSpeakingQueueRepository;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeakingQueueService {

    private final RedisSpeakingQueueRepository redisSpeakingQueueRepository;
    private final SpeakingQueuePersistenceService speakingQueuePersistenceService;

    public StageRequestRes requestSpeakingTurn(Long roomId, Long userId) {
        SpeakingQueue saved =
                speakingQueuePersistenceService.createWaitingRequest(roomId, userId);

        Optional<SpeakingQueue> assigned = assignNextSpeaker(roomId);
        Optional<SpeakingQueue> assignedRequest =
                assigned.filter(value -> isSameRequest(value, saved));
        if (assignedRequest.isPresent()) {
            return StageRequestRes.from(assignedRequest.get());
        }

        synchronizeWaitingRedisProjection(saved);
        return StageRequestRes.from(saved);
    }

    public Optional<SpeakingQueue> assignNextSpeaker(Long roomId) {
        Optional<SpeakingQueue> assigned =
                speakingQueuePersistenceService.assignNextSpeaker(roomId);
        assigned.ifPresent(this::synchronizeAssignedRedisProjection);
        return assigned;
    }

    private void synchronizeWaitingRedisProjection(SpeakingQueue speakingQueue) {
        try {
            redisSpeakingQueueRepository.upsert(
                    speakingQueue.getRoomId(),
                    speakingQueue.getUserId(),
                    speakingQueue.getQueueOrder()
            );
        } catch (RuntimeException synchronizationException) {
            log.error(
                    "Failed to synchronize durable speaking request to Redis. "
                            + "roomId={}, userId={}, queueOrder={}",
                    speakingQueue.getRoomId(),
                    speakingQueue.getUserId(),
                    speakingQueue.getQueueOrder(),
                    synchronizationException
            );
        }
    }

    private void synchronizeAssignedRedisProjection(SpeakingQueue speakingQueue) {
        try {
            redisSpeakingQueueRepository.assign(
                    speakingQueue.getRoomId(),
                    speakingQueue.getUserId()
            );
        } catch (RuntimeException synchronizationException) {
            log.error(
                    "Failed to synchronize assigned speaker to Redis. "
                            + "roomId={}, userId={}, queueOrder={}",
                    speakingQueue.getRoomId(),
                    speakingQueue.getUserId(),
                    speakingQueue.getQueueOrder(),
                    synchronizationException
            );
        }
    }

    private boolean isSameRequest(
            SpeakingQueue first,
            SpeakingQueue second
    ) {
        return Objects.equals(first.getRoomId(), second.getRoomId())
                && Objects.equals(first.getUserId(), second.getUserId())
                && Objects.equals(first.getQueueOrder(), second.getQueueOrder());
    }
}
