package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SpeakingQueuePersistenceService {

    private static final List<SpeakingQueueStatus> ACTIVE_STATUSES =
            List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED);

    private final SpeakingQueueRepository speakingQueueRepository;

    @Transactional
    public SpeakingQueue createWaitingRequest(Long roomId, Long userId) {
        if (speakingQueueRepository.existsByRoomIdAndUserIdAndStatusIn(
                roomId,
                userId,
                ACTIVE_STATUSES
        )) {
            throw new CustomException(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS);
        }

        SpeakingQueue speakingQueue = SpeakingQueue.waiting(
                roomId,
                userId,
                LocalDateTime.now()
        );
        SpeakingQueue saved = speakingQueueRepository.saveAndFlush(speakingQueue);
        saved.assignQueueOrderFromId();
        return saved;
    }

    @Transactional
    public Optional<SpeakingQueue> assignNextSpeaker(Long roomId) {
        Optional<SpeakingQueue> waitingRequest =
                speakingQueueRepository
                        .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                                roomId,
                                SpeakingQueueStatus.WAITING
                        );

        if (waitingRequest.isEmpty()) {
            return Optional.empty();
        }

        if (speakingQueueRepository.existsByRoomIdAndStatus(
                roomId,
                SpeakingQueueStatus.ASSIGNED
        )) {
            return Optional.empty();
        }

        SpeakingQueue nextSpeaker = waitingRequest.get();
        nextSpeaker.assign();
        return Optional.of(nextSpeaker);
    }
}
