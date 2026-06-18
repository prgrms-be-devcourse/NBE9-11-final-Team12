package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.projection.CurrentSpeakerProjection;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
    private final UserRepository userRepository;

    @Transactional
    public SpeakingQueue createWaitingRequest(
            Long roomId,
            Long userId,
            SpeechStance stance
    ) {
        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (speakingQueueRepository.existsByRoomIdAndUserIdAndStatusIn(
                roomId,
                userId,
                ACTIVE_STATUSES
        )) {
            throw new CustomException(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS);
        }

        int nextQueueOrder =
                speakingQueueRepository.findMaxQueueOrderByRoomId(roomId) + 1;
        SpeakingQueue speakingQueue = SpeakingQueue.waiting(
                roomId,
                userId,
                nextQueueOrder,
                stance,
                LocalDateTime.now()
        );
        return speakingQueueRepository.save(speakingQueue);
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

    @Transactional(readOnly = true)
    public Optional<SpeakingQueue> findMyActiveRequest(Long roomId, Long userId) {
        if (!roomRepository.existsById(roomId)) {
            throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
        }

        return speakingQueueRepository.findByRoomIdAndUserIdAndStatusIn(
                roomId,
                userId,
                ACTIVE_STATUSES
        );
    }

    @Transactional(readOnly = true)
    public void validateRoomExists(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
        }
    }

    @Transactional(readOnly = true)
    public Map<Long, String> findNicknamesByUserIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(
                        user -> user.getId(),
                        user -> user.getNickname(),
                        (first, second) -> first
                ));
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

    @Transactional(readOnly = true)
    public Optional<CurrentSpeakerProjection> findCurrentSpeaker(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
        }

        return speakingQueueRepository.findCurrentSpeakerProjection(
                roomId,
                SpeakingQueueStatus.ASSIGNED
        );
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
