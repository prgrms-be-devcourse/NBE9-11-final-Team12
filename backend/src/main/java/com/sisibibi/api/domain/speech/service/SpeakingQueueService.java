package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.speech.config.SpeakingQueueProperties;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.repository.RedisSpeakingQueueRepository;
import java.time.LocalDateTime;
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
    private final SpeakingQueueProperties speakingQueueProperties;

    public StageRequestRes requestSpeakingTurn(Long roomId, Long userId) {
        SpeakingQueue saved =
                speakingQueuePersistenceService.createWaitingRequest(roomId, userId);

        synchronizeWaitingRedisProjection(saved);
        return StageRequestRes.from(saved);
    }

    public Optional<SpeakingQueue> assignNextSpeaker(Long roomId) {
        LocalDateTime assignedAt = LocalDateTime.now();
        Optional<SpeakingQueue> assigned =
                speakingQueuePersistenceService.assignNextSpeaker(
                        roomId,
                        assignedAt,
                        assignedAt.plus(speakingQueueProperties.getTurnDuration())
                );
        assigned.ifPresent(this::synchronizeAssignedRedisProjection);
        return assigned;
    }

    public void cancelSpeakingRequest(Long roomId, Long userId) {
        SpeakingQueue canceled =
                speakingQueuePersistenceService.cancelWaitingRequest(roomId, userId);
        synchronizeCanceledRedisProjection(canceled);
    }

    public void completeSpeakingTurn(Long roomId, Long userId) {
        SpeakingQueue completed =
                speakingQueuePersistenceService.completeCurrentSpeaker(roomId, userId);
        synchronizeCompletedRedisProjection(completed);
    }

    public Optional<SpeakingQueue> expireCurrentSpeaker(
            Long roomId,
            LocalDateTime now
    ) {
        Optional<SpeakingQueue> expired =
                speakingQueuePersistenceService.expireCurrentSpeaker(roomId, now);
        expired.ifPresent(this::synchronizeCompletedRedisProjection);
        return expired;
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

    private void synchronizeCanceledRedisProjection(SpeakingQueue speakingQueue) {
        try {
            redisSpeakingQueueRepository.remove(
                    speakingQueue.getRoomId(),
                    speakingQueue.getUserId()
            );
        } catch (RuntimeException synchronizationException) {
            log.error(
                    "Failed to remove canceled speaking request from Redis. "
                            + "roomId={}, userId={}, queueOrder={}",
                    speakingQueue.getRoomId(),
                    speakingQueue.getUserId(),
                    speakingQueue.getQueueOrder(),
                    synchronizationException
            );
        }
    }

    private void synchronizeCompletedRedisProjection(SpeakingQueue speakingQueue) {
        try {
            redisSpeakingQueueRepository.removeCurrentSpeaker(
                    speakingQueue.getRoomId(),
                    speakingQueue.getUserId()
            );
        } catch (RuntimeException synchronizationException) {
            log.error(
                    "Failed to remove completed current speaker from Redis. "
                            + "roomId={}, userId={}, queueOrder={}",
                    speakingQueue.getRoomId(),
                    speakingQueue.getUserId(),
                    speakingQueue.getQueueOrder(),
                    synchronizationException
            );
        }
    }

}
