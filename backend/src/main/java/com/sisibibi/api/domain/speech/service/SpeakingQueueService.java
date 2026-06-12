package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.dto.response.CurrentSpeakerRes;
import com.sisibibi.api.domain.speech.dto.response.StageCompleteRes;
import com.sisibibi.api.domain.speech.dto.response.StageExpireRes;
import com.sisibibi.api.domain.speech.dto.response.StageQueueRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @Transactional
    public StageRequestRes requestSpeakingTurn(Long roomId, Long userId) {
        User user = getUser(userId);
        user.validateCanRequestSpeakingTurn();

        Room room = lockRoom(roomId);
        room.validateOpen();

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
        lockRoom(roomId);

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

    @Transactional
    public CurrentSpeakerRes assignNextSpeaker(Long roomId) {
        Room room = lockRoom(roomId);
        room.validateOpen();

        if (speakingQueueRepository.existsByRoomIdAndStatus(
                roomId,
                SpeakingQueueStatus.ASSIGNED
        )) {
            throw new CustomException(ErrorCode.CURRENT_SPEAKER_ALREADY_EXISTS);
        }

        SpeakingQueue nextSpeaker = speakingQueueRepository
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        roomId,
                        SpeakingQueueStatus.WAITING
                )
                .orElseThrow(() -> new CustomException(ErrorCode.SPEAKING_QUEUE_EMPTY));

        nextSpeaker.assign(LocalDateTime.now());

        return CurrentSpeakerRes.from(nextSpeaker);
    }

    @Transactional
    public StageCompleteRes completeCurrentSpeaker(Long roomId, Long userId) {
        Room room = lockRoom(roomId);
        room.validateOpen();

        SpeakingQueue currentSpeaker = speakingQueueRepository
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        roomId,
                        SpeakingQueueStatus.ASSIGNED
                )
                .orElseThrow(() -> new CustomException(ErrorCode.CURRENT_SPEAKER_NOT_FOUND));

        if (!currentSpeaker.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        LocalDateTime now = LocalDateTime.now();

        currentSpeaker.complete(now);

        SpeakingQueue nextSpeaker = speakingQueueRepository
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        roomId,
                        SpeakingQueueStatus.WAITING
                )
                .orElse(null);

        if (nextSpeaker != null) {
            nextSpeaker.assign(now);
        }

        return StageCompleteRes.of(roomId, currentSpeaker, nextSpeaker);
    }

    @Transactional
    public StageExpireRes expireCurrentSpeakerIfTimedOut(
            Long roomId,
            LocalDateTime now,
            Duration speakingTimeLimit
    ) {
        Room room = lockRoom(roomId);
        room.validateOpen();

        SpeakingQueue currentSpeaker = speakingQueueRepository
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        roomId,
                        SpeakingQueueStatus.ASSIGNED
                )
                .orElseThrow(() -> new CustomException(ErrorCode.CURRENT_SPEAKER_NOT_FOUND));

        if (currentSpeaker.getAssignedAt().plus(speakingTimeLimit).isAfter(now)) {
            throw new CustomException(ErrorCode.SPEAKING_TIME_NOT_EXPIRED);
        }

        currentSpeaker.expire(now);

        SpeakingQueue nextSpeaker = speakingQueueRepository
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        roomId,
                        SpeakingQueueStatus.WAITING
                )
                .orElse(null);

        if (nextSpeaker != null) {
            nextSpeaker.assign(now);
        }

        return StageExpireRes.of(roomId, currentSpeaker, nextSpeaker);
    }

    @Transactional(readOnly = true)
    public CurrentSpeakerRes getCurrentSpeaker(Long roomId) {
        SpeakingQueue currentSpeaker = speakingQueueRepository
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        roomId,
                        SpeakingQueueStatus.ASSIGNED
                )
                .orElseThrow(() -> new CustomException(ErrorCode.CURRENT_SPEAKER_NOT_FOUND));

        return CurrentSpeakerRes.from(currentSpeaker);
    }

    private Room lockRoom(Long roomId) {
        return roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
