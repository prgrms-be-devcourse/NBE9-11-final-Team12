package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.entity.RoomQueueSequence;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;
import com.sisibibi.api.domain.speech.repository.RoomQueueSequenceRepository;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.repository.projection.CurrentSpeakerProjection;
import com.sisibibi.api.domain.speech.repository.projection.SpeakingRequestEligibilityProjection;
import com.sisibibi.api.domain.speech.util.SpeakingStreakPolicy;
import com.sisibibi.api.domain.user.repository.UserRepository;
import com.sisibibi.api.domain.usersanction.service.UserSanctionPolicyService;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SpeakingQueuePersistenceService {

    private static final List<SpeakingQueueStatus> ACTIVE_STATUSES =
            List.of(SpeakingQueueStatus.WAITING, SpeakingQueueStatus.ASSIGNED);
    private static final List<SpeakingQueueStatus> ASSIGNMENT_HISTORY_STATUSES =
            List.of(SpeakingQueueStatus.COMPLETED);

    private final SpeakingQueueRepository speakingQueueRepository;
    private final RoomQueueSequenceRepository roomQueueSequenceRepository;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final UserRepository userRepository;
    private final UserSanctionPolicyService userSanctionPolicyService;
    private final SpeakingStreakPolicy speakingStreakPolicy;
    private final SpeechRepository speechRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SpeakingQueue createWaitingRequest(
            Long roomId,
            Long userId,
            SpeechStance stance
    ) {
        LocalDateTime requestedAt = LocalDateTime.now();
        validateSpeakingRequestEligibility(roomId, userId, requestedAt);

        int nextQueueOrder = issueNextQueueOrder(roomId, requestedAt);
        SpeakingQueue speakingQueue = SpeakingQueue.waiting(
                roomId,
                userId,
                nextQueueOrder,
                stance,
                requestedAt
        );
        try {
            speakingQueueRepository.insertWaitingRequest(
                    roomId,
                    userId,
                    nextQueueOrder,
                    stance == null ? null : stance.name(),
                    SpeakingQueueStatus.WAITING.name(),
                    requestedAt
            );
            return speakingQueue;
        } catch (DataIntegrityViolationException duplicateRequestException) {
            if (speakingQueueRepository.existsByRoomIdAndUserIdAndStatusIn(
                    roomId,
                    userId,
                    ACTIVE_STATUSES
            )) {
                throw new CustomException(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS);
            }
            throw duplicateRequestException;
        }
    }

    private int issueNextQueueOrder(Long roomId, LocalDateTime requestedAt) {
        int updatedRows = roomQueueSequenceRepository.issueNextQueueOrderIfRoomActive(
                roomId,
                requestedAt
        );
        if (updatedRows == 0) {
            validateRoomActiveForSpeakingRequest(roomId, requestedAt);
            throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
        }

        return roomQueueSequenceRepository.findNextQueueOrderByRoomId(roomId)
                .map(nextQueueOrder -> nextQueueOrder - 1)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
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
    public long countWaitingRequests(Long roomId) {
        return speakingQueueRepository.countByRoomIdAndStatus(
                roomId,
                SpeakingQueueStatus.WAITING
        );
    }

    @Transactional(readOnly = true)
    public long countWaitingRequestsBefore(Long roomId, Integer queueOrder) {
        return speakingQueueRepository.countByRoomIdAndStatusAndQueueOrderLessThan(
                roomId,
                SpeakingQueueStatus.WAITING,
                queueOrder
        );
    }

    @Transactional(readOnly = true)
    public List<SpeakingQueue> findWaitingRequestsForRedisReadFallback(
            Long roomId,
            int offset,
            int size
    ) {
        return speakingQueueRepository.findWaitingPageForRedisReadFallback(
                roomId,
                SpeakingQueueStatus.WAITING.name(),
                offset,
                size
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
    public SpeakingQueue validateCurrentSpeaker(Long roomId, Long userId) {
        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        SpeakingQueue currentSpeaker = speakingQueueRepository
                .findByRoomIdAndStatus(roomId, SpeakingQueueStatus.ASSIGNED)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.CURRENT_SPEAKER_NOT_FOUND));

        if (!currentSpeaker.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return currentSpeaker;
    }

    @Transactional
    public void recordCurrentSpeakerActivityIfMatches(
            Long roomId,
            Long userId,
            LocalDateTime activityAt
    ) {
        Optional<SpeakingQueue> activeRequest =
                speakingQueueRepository.findByRoomIdAndUserIdAndStatusIn(
                        roomId,
                        userId,
                        List.of(SpeakingQueueStatus.ASSIGNED)
                );
        if (activeRequest.isEmpty()) {
            return;
        }

        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        Optional<SpeakingQueue> currentSpeaker =
                speakingQueueRepository.findByRoomIdAndStatus(
                        roomId,
                        SpeakingQueueStatus.ASSIGNED
                );
        if (currentSpeaker.isEmpty()
                || !currentSpeaker.get().getUserId().equals(userId)) {
            return;
        }

        currentSpeaker.get().recordActivity(activityAt);
    }

    @Transactional
    public Optional<SpeakingQueue> warnCurrentSpeakerIfIdle(
            Long roomId,
            LocalDateTime now,
            Duration warningDelay,
            Duration warningSuppressionBeforeExpiration
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
        if (!assigned.markIdleWarningIfDue(
                now,
                warningDelay,
                warningSuppressionBeforeExpiration
        )) {
            return Optional.empty();
        }

        return Optional.of(assigned);
    }

    @Transactional
    public SpeakingQueueRoomCloseResult closeActiveRequestsByRoomId(
            Long roomId,
            LocalDateTime closedAt
    ) {
        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        lockQueueSequenceIfExists(roomId);
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
                completeSpeakingQueueAndSpeeches(speakingQueue, closedAt);
                completedRequests.add(speakingQueue);
            }
        }

        return SpeakingQueueRoomCloseResult.of(canceledRequests, completedRequests);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SpeakingQueueAssignmentResult assignNextSpeaker(
            Long roomId,
            LocalDateTime assignedAt,
            LocalDateTime expiresAt
    ) {
        if (hasAssignedSpeaker(roomId)) {
            return SpeakingQueueAssignmentResult.empty();
        }

        Room room = findRoomForUpdate(roomId);
        if (!room.isActiveAt(assignedAt)) {
            return SpeakingQueueAssignmentResult.empty();
        }

        if (hasAssignedSpeaker(roomId)) {
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

    private boolean hasAssignedSpeaker(Long roomId) {
        return speakingQueueRepository.existsByRoomIdAndStatus(
                roomId,
                SpeakingQueueStatus.ASSIGNED
        );
    }

    private void validateJoinedParticipant(Long roomId, Long userId) {
        if (!isJoinedParticipant(roomId, userId)) {
            throw new CustomException(ErrorCode.ROOM_PARTICIPATION_REQUIRED);
        }
    }

    private void validateNoActiveRequest(Long roomId, Long userId) {
        if (speakingQueueRepository.existsByRoomIdAndUserIdAndStatusIn(
                roomId,
                userId,
                ACTIVE_STATUSES
        )) {
            throw new CustomException(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS);
        }
    }

    private void validateSpeakingRequestEligibility(
            Long roomId,
            Long userId,
            LocalDateTime requestedAt
    ) {
        SpeakingRequestEligibilityProjection eligibility =
                speakingQueueRepository.findSpeakingRequestEligibility(
                        roomId,
                        userId,
                        requestedAt
                );

        if (eligibility == null || !isTrue(eligibility.getRoomExists())) {
            throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
        }
        if (!isTrue(eligibility.getRoomActive())) {
            throw new CustomException(ErrorCode.ROOM_CLOSED);
        }
        if (isTrue(eligibility.getRestricted())) {
            throw new CustomException(ErrorCode.USER_STAGE_RESTRICTED);
        }
        if (!isTrue(eligibility.getJoinedParticipant())) {
            throw new CustomException(ErrorCode.ROOM_PARTICIPATION_REQUIRED);
        }
        if (isTrue(eligibility.getActiveRequestExists())) {
            throw new CustomException(ErrorCode.SPEAKING_REQUEST_ALREADY_EXISTS);
        }
    }

    private void validateRoomActiveForSpeakingRequest(
            Long roomId,
            LocalDateTime requestedAt
    ) {
        if (!isTrue(speakingQueueRepository.findRoomActiveForSpeakingRequest(
                roomId,
                requestedAt
        ))) {
            throw new CustomException(ErrorCode.ROOM_CLOSED);
        }
    }

    private boolean isTrue(Integer value) {
        return value != null && value == 1;
    }

    private Room findRoomForUpdate(Long roomId) {
        return roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }

    private Room findRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }

    private RoomQueueSequence findQueueSequenceForUpdate(Long roomId) {
        return roomQueueSequenceRepository.findByRoomIdForUpdate(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }

    private void lockQueueSequenceIfExists(Long roomId) {
        roomQueueSequenceRepository.findByRoomIdForUpdate(roomId);
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

        Optional<SpeechStance> oppositeStance =
                speakingStreakPolicy.counterStanceFor(recentAssignments);
        if (oppositeStance.isEmpty()) {
            return Optional.empty();
        }

        return speakingQueueRepository
                .findFirstByRoomIdAndStatusAndStanceOrderByQueueOrderAsc(
                        roomId,
                        SpeakingQueueStatus.WAITING,
                        oppositeStance.get()
                );
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

        LocalDateTime completedAt = LocalDateTime.now();
        completeSpeakingQueueAndSpeeches(currentSpeaker, completedAt);
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
        LocalDateTime completedAt = LocalDateTime.now();
        completeSpeakingQueueAndSpeeches(assigned, completedAt);
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

        completeSpeakingQueueAndSpeeches(assigned, now);
        return Optional.of(assigned);
    }

    @Transactional
    public Optional<SpeakingQueue> completeCurrentSpeakerIfIdleTimedOut(
            Long roomId,
            LocalDateTime now,
            Duration timeoutDelayAfterWarning
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
        if (!assigned.isIdleTimedOut(now, timeoutDelayAfterWarning)) {
            return Optional.empty();
        }

        completeSpeakingQueueAndSpeeches(assigned, now);
        return Optional.of(assigned);
    }

    private void completeSpeakingQueueAndSpeeches(
            SpeakingQueue speakingQueue,
            LocalDateTime endedAt
    ) {
        speakingQueue.complete();
        completeSpeakingSpeeches(speakingQueue, endedAt);
    }

    private void completeSpeakingSpeeches(SpeakingQueue speakingQueue, LocalDateTime endedAt) {
        speechRepository.completeSpeakingSpeeches(
                speakingQueue.getRoomId(),
                speakingQueue.getUserId(),
                SpeechStatus.SPEAKING,
                SpeechStatus.COMPLETED,
                endedAt
        );
    }
}
