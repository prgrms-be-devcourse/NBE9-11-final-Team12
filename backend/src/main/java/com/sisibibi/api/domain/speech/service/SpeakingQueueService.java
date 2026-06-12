package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.speech.dto.response.StageQueueRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SpeakingQueueService {

    private static final List<SpeakingQueueStatus> ACTIVE_STATUSES = List.of(
            SpeakingQueueStatus.WAITING,
            SpeakingQueueStatus.ASSIGNED
    );

    private final SpeakingQueueRepository speakingQueueRepository;

    @Transactional
    public StageRequestRes requestSpeakingTurn(Long roomId, Long userId) {
        if (speakingQueueRepository.existsByRoomIdAndUserIdAndStatusIn(
                roomId,
                userId,
                ACTIVE_STATUSES
        )) {
            throw new CustomException(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS);
        }

        int nextQueueOrder = speakingQueueRepository.findMaxQueueOrderByRoomId(roomId) + 1;

        SpeakingQueue speakingQueue = SpeakingQueue.create(
                roomId,
                userId,
                nextQueueOrder,
                LocalDateTime.now()
        );

        return StageRequestRes.from(speakingQueueRepository.save(speakingQueue));
    }

    @Transactional(readOnly = true)
    public StageRequestRes getMyRequest(Long roomId, Long userId) {
        SpeakingQueue speakingQueue = speakingQueueRepository
                .findFirstByRoomIdAndUserIdOrderByIdDesc(roomId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SPEAKING_REQUEST_NOT_FOUND));

        return StageRequestRes.from(speakingQueue);
    }

    @Transactional
    public void cancelMyRequest(Long roomId, Long userId) {
        SpeakingQueue speakingQueue = speakingQueueRepository
                .findFirstByRoomIdAndUserIdAndStatusOrderByQueueOrderAsc(
                        roomId,
                        userId,
                        SpeakingQueueStatus.WAITING
                )
                .orElseThrow(() -> new CustomException(ErrorCode.SPEAKING_REQUEST_NOT_FOUND));

        speakingQueue.cancel(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public StageQueueRes getWaitingQueue(Long roomId) {
        List<SpeakingQueue> speakingQueues = speakingQueueRepository
                .findByRoomIdAndStatusOrderByQueueOrderAsc(roomId, SpeakingQueueStatus.WAITING);

        return StageQueueRes.of(roomId, speakingQueues);
    }
}
