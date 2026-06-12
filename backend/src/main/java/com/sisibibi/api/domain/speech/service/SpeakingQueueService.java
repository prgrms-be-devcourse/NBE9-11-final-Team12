package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.repository.RedisSpeakingQueueRepository;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalInt;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeakingQueueService {

    private static final List<SpeakingQueueStatus> ACTIVE_STATUSES =
            List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED);

    private final RedisSpeakingQueueRepository speakingQueueRepository;
    private final SpeakingQueueRepository speakingQueueJpaRepository;

    @Transactional
    public StageRequestRes requestSpeakingTurn(Long roomId, Long userId) {
        if (speakingQueueJpaRepository.existsByRoomIdAndUserIdAndStatusIn(
                roomId,
                userId,
                ACTIVE_STATUSES
        )) {
            throw new CustomException(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS);
        }

        OptionalInt queueOrder = speakingQueueRepository.enqueue(roomId, userId);

        if (queueOrder.isEmpty()) {
            throw new CustomException(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS);
        }

        SpeakingQueue speakingQueue = SpeakingQueue.waiting(
                roomId,
                userId,
                queueOrder.getAsInt(),
                LocalDateTime.now()
        );

        try {
            SpeakingQueue saved = speakingQueueJpaRepository.saveAndFlush(speakingQueue);
            return StageRequestRes.from(saved);
        } catch (RuntimeException e) {
            compensateRedisRegistration(roomId, userId);
            throw e;
        }
    }

    private void compensateRedisRegistration(Long roomId, Long userId) {
        try {
            speakingQueueRepository.remove(roomId, userId);
        } catch (RuntimeException compensationException) {
            log.error(
                    "Failed to compensate Redis speaking queue registration. roomId={}, userId={}",
                    roomId,
                    userId,
                    compensationException
            );
        }
    }
}
