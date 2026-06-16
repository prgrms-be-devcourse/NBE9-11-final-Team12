package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.repository.RoomRepository;
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
    private final RoomRepository roomRepository;

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
    public SpeakingQueue cancelWaitingRequest(Long roomId, Long userId) {
        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        SpeakingQueue speakingQueue = speakingQueueRepository
                .findByRoomIdAndUserIdAndStatusIn(
                        roomId,
                        userId,
                        ACTIVE_STATUSES
                )
                .orElseThrow(() ->
                        new CustomException(ErrorCode.SPEAKING_REQUEST_NOT_FOUND));

        if (speakingQueue.getStatus() != SpeakingQueueStatus.WAITING) {
            throw new CustomException(ErrorCode.SPEAKING_REQUEST_NOT_CANCELABLE);
        }

        speakingQueue.cancel(LocalDateTime.now());
        return speakingQueue;
    }

    @Transactional
    public Optional<SpeakingQueue> assignNextSpeaker(
            Long roomId,
            LocalDateTime assignedAt,
            LocalDateTime expiresAt
    ) {
        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (speakingQueueRepository.existsByRoomIdAndStatus(
                roomId,
                SpeakingQueueStatus.ASSIGNED
        )) {
            return Optional.empty();
        }

        Optional<SpeakingQueue> waitingRequest =
                speakingQueueRepository
                        .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                                roomId,
                                SpeakingQueueStatus.WAITING
                        );

        if (waitingRequest.isEmpty()) {
            return Optional.empty();
        }

        SpeakingQueue nextSpeaker = waitingRequest.get();
        nextSpeaker.assign(assignedAt, expiresAt);
        return Optional.of(nextSpeaker);
    }

    @Transactional
    public SpeakingQueue completeCurrentSpeaker(Long roomId, Long userId) {
        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        SpeakingQueue currentSpeaker = speakingQueueRepository
                .findByRoomIdAndStatus(roomId, SpeakingQueueStatus.ASSIGNED)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.CURRENT_SPEAKER_NOT_FOUND));

        if (!currentSpeaker.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        currentSpeaker.complete();
        return currentSpeaker;
    }

    @Transactional
    public Optional<SpeakingQueue> expireCurrentSpeaker(
            Long roomId,
            LocalDateTime now
    ) {
        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        Optional<SpeakingQueue> currentSpeaker =
                speakingQueueRepository.findByRoomIdAndStatus(
                        roomId,
                        SpeakingQueueStatus.ASSIGNED
                );

        if (currentSpeaker.isEmpty()) {
            return Optional.empty();
        }

        SpeakingQueue assigned = currentSpeaker.get();
        if (assigned.getExpiresAt() == null || assigned.getExpiresAt().isAfter(now)) {
            return Optional.empty();
        }

        assigned.complete();
        return Optional.of(assigned);
    }
}
