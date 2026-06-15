package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.repository.RedisSpeakingQueueRepository;
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
        synchronizeRedisProjection(saved);
        return StageRequestRes.from(saved);
    }

    private void synchronizeRedisProjection(SpeakingQueue speakingQueue) {
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
}
