package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.projection.CurrentSpeakerProjection;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.domain.usersanction.service.UserSanctionPolicyService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private static final List<SpeakingQueueStatus> ASSIGNMENT_HISTORY_STATUSES =
            List.of(SpeakingQueueStatus.COMPLETED);
    private static final int BALANCE_STREAK_THRESHOLD = 3;

    private final SpeakingQueueRepository speakingQueueRepository;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final UserRepository userRepository;
    private final UserSanctionPolicyService userSanctionPolicyService;

    @Transactional
    public SpeakingQueue createWaitingRequest(
            Long roomId,
            Long userId,
            SpeechStance stance
    ) {
        userSanctionPolicyService.validateStageAllowed(userId);

        LocalDateTime requestedAt = LocalDateTime.now();
        Room room = findRoomForUpdate(roomId);
        validateRoomActive(room, requestedAt);
        validateJoinedParticipant(roomId, userId);

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
                requestedAt
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

    @Transactional(readOnly = true)
    public List<SpeakingQueue> findWaitingRequestsForRedisProjection(Long roomId) {
        return speakingQueueRepository.findByRoomIdAndStatusOrderByQueueOrderAsc(
                roomId,
                SpeakingQueueStatus.WAITING
        );
    }

    @Transactional(readOnly = true)
    public Optional<SpeakingQueue> findCurrentSpeakerForRedisProjection(Long roomId) {
        return speakingQueueRepository.findByRoomIdAndStatus(
                roomId,
                SpeakingQueueStatus.ASSIGNED
        );
    }

    @Transactional
    public SpeakingQueueRoomCloseResult closeActiveRequestsByRoomId(
            Long roomId,
            LocalDateTime closedAt
    ) {
        List<SpeakingQueue> activeRequests =
                speakingQueueRepository.findByRoomIdAndStatusInOrderByQueueOrderAsc(
                        roomId,
                        ACTIVE_STATUSES
                );
        List<SpeakingQueue> canceledRequests = new ArrayList<>();
        List<SpeakingQueue> completedRequests = new ArrayList<>();

        for (SpeakingQueue speakingQueue : activeRequests) {
            if (speakingQueue.getStatus() == SpeakingQueueStatus.WAITING) {
                speakingQueue.cancel(closedAt);
                canceledRequests.add(speakingQueue);
                continue;
            }

            if (speakingQueue.getStatus() == SpeakingQueueStatus.ASSIGNED) {
                speakingQueue.complete();
                completedRequests.add(speakingQueue);
            }
        }

        return SpeakingQueueRoomCloseResult.of(canceledRequests, completedRequests);
    }

    @Transactional
    public SpeakingQueueAssignmentResult assignNextSpeaker(
            Long roomId,
            LocalDateTime assignedAt,
            LocalDateTime expiresAt
    ) {
        Room room = findRoomForUpdate(roomId);
        if (!room.isActiveAt(assignedAt)) {
            return SpeakingQueueAssignmentResult.empty();
        }

        if (speakingQueueRepository.existsByRoomIdAndStatus(
                roomId,
                SpeakingQueueStatus.ASSIGNED
        )) {
            return SpeakingQueueAssignmentResult.empty();
        }

        List<SpeakingQueue> canceledRequests = new ArrayList<>();
        Optional<SpeakingQueue> waitingRequest = findNextWaitingRequest(roomId);
        while (waitingRequest.isPresent()) {
            SpeakingQueue nextSpeaker = waitingRequest.get();
            if (!isJoinedParticipant(roomId, nextSpeaker.getUserId())) {
                nextSpeaker.cancel(assignedAt);
                canceledRequests.add(nextSpeaker);
                waitingRequest = findNextWaitingRequest(roomId);
                continue;
            }

            nextSpeaker.assign(assignedAt, expiresAt);
            return SpeakingQueueAssignmentResult.of(Optional.of(nextSpeaker), canceledRequests);
        }

        return SpeakingQueueAssignmentResult.of(Optional.empty(), canceledRequests);
    }

    private void validateJoinedParticipant(Long roomId, Long userId) {
        if (!isJoinedParticipant(roomId, userId)) {
            throw new CustomException(ErrorCode.ROOM_PARTICIPATION_REQUIRED);
        }
    }

    private Room findRoomForUpdate(Long roomId) {
        return roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }

    private void validateRoomActive(Room room, LocalDateTime now) {
        if (!room.isActiveAt(now)) {
            throw new CustomException(ErrorCode.ROOM_CLOSED);
        }
    }

    private boolean isJoinedParticipant(Long roomId, Long userId) {
        return roomParticipantRepository.existsByRoomIdAndUserIdAndStatus(
                roomId,
                userId,
                RoomParticipantStatus.JOINED
        );
    }

    private Optional<SpeakingQueue> findNextWaitingRequest(Long roomId) {
        Optional<SpeakingQueue> balancedRequest = findOppositeStanceWaitingRequest(roomId);
        if (balancedRequest.isPresent()) {
            return balancedRequest;
        }

        return speakingQueueRepository
                .findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
                        roomId,
                        SpeakingQueueStatus.WAITING
                );
    }

    private Optional<SpeakingQueue> findOppositeStanceWaitingRequest(Long roomId) {
        List<SpeakingQueue> recentAssignments =
                speakingQueueRepository
                        .findTop3ByRoomIdAndStatusInAndStanceIsNotNullOrderByAssignedAtDesc(
                                roomId,
                                ASSIGNMENT_HISTORY_STATUSES
                        );

        if (recentAssignments.size() < BALANCE_STREAK_THRESHOLD) {
            return Optional.empty();
        }

        SpeechStance recentStance = recentAssignments.getFirst().getStance();
        boolean sameStanceStreak = recentAssignments.stream()
                .allMatch(assignment -> recentStance == assignment.getStance());
        if (!sameStanceStreak) {
            return Optional.empty();
        }

        return speakingQueueRepository
                .findFirstByRoomIdAndStatusAndStanceOrderByQueueOrderAsc(
                        roomId,
                        SpeakingQueueStatus.WAITING,
                        oppositeOf(recentStance)
                );
    }

    private SpeechStance oppositeOf(SpeechStance stance) {
        if (stance == SpeechStance.PRO) {
            return SpeechStance.CON;
        }
        return SpeechStance.PRO;
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
    public Optional<SpeakingQueue> completeCurrentSpeakerIfMatches(Long roomId, Long userId) {
        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        Optional<SpeakingQueue> currentSpeaker =
                speakingQueueRepository.findByRoomIdAndStatus(
                        roomId,
                        SpeakingQueueStatus.ASSIGNED
                );

        if (currentSpeaker.isEmpty()
                || !currentSpeaker.get().getUserId().equals(userId)) {
            return Optional.empty();
        }

        SpeakingQueue assigned = currentSpeaker.get();
        assigned.complete();
        return Optional.of(assigned);
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
