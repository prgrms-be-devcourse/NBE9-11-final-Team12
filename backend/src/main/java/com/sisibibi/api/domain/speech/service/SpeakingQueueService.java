package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.speech.config.SpeakingQueueProperties;
import com.sisibibi.api.domain.speech.dto.event.StageChangedEvent;
import com.sisibibi.api.domain.speech.dto.event.StageEventPayload;
import com.sisibibi.api.domain.speech.dto.event.StageEventType;
import com.sisibibi.api.domain.speech.dto.event.StageTurnEndReason;
import com.sisibibi.api.domain.speech.dto.response.StageCurrentSpeakerRes;
import com.sisibibi.api.domain.speech.dto.response.StageQueueRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestRes;
import com.sisibibi.api.domain.speech.dto.response.StageRequestStatusRes;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.RedisSpeakingQueueRepository;
import com.sisibibi.api.domain.speech.repository.projection.CurrentSpeakerProjection;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpeakingQueueService {

    private static final int REDIS_PROJECTION_REBUILD_MAX_ATTEMPTS = 3;
    private static final long REDIS_PROJECTION_REBUILD_INITIAL_BACKOFF_MS = 50L;
    private static final int REDIS_PROJECTION_REBUILD_BACKOFF_MULTIPLIER = 2;

    private final RedisSpeakingQueueRepository redisSpeakingQueueRepository;
    private final SpeakingQueuePersistenceService speakingQueuePersistenceService;
    private final SpeakingQueueProperties speakingQueueProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final AiCounterIssueService aiCounterIssueService;

    public StageRequestRes requestSpeakingTurn(
            Long roomId,
            Long userId,
            SpeechStance stance
    ) {
        SpeakingQueue saved =
                speakingQueuePersistenceService.createWaitingRequest(roomId, userId, stance);

        synchronizeWaitingRedisProjection(saved);
        log.info(
                "Speaking request created. roomId={}, userId={}, queueOrder={}, status={}",
                saved.getRoomId(),
                saved.getUserId(),
                saved.getQueueOrder(),
                saved.getStatus()
        );
        publishStageChanged(StageEventType.SPEAKING_REQUESTED, saved);
        return StageRequestRes.from(
                tryAssignNextSpeaker(roomId)
                        .filter(assigned -> assigned.getUserId().equals(userId))
                        .orElse(saved)
        );
    }

    public Optional<SpeakingQueue> assignNextSpeaker(Long roomId) {
        LocalDateTime assignedAt = LocalDateTime.now();
        SpeakingQueueAssignmentResult assignmentResult =
                speakingQueuePersistenceService.assignNextSpeaker(
                        roomId,
                        assignedAt,
                        assignedAt.plus(speakingQueueProperties.getTurnDuration())
                );
        assignmentResult.canceledRequests().forEach(this::synchronizeCanceledRedisProjection);
        assignmentResult.canceledRequests().forEach(speakingQueue -> log.info(
                "Speaking request canceled because participant left room. "
                        + "roomId={}, userId={}, queueOrder={}",
                speakingQueue.getRoomId(),
                speakingQueue.getUserId(),
                speakingQueue.getQueueOrder()
        ));
        assignmentResult.canceledRequests().forEach(speakingQueue ->
                publishStageChanged(StageEventType.SPEAKING_CANCELED, speakingQueue));

        Optional<SpeakingQueue> assigned = assignmentResult.assignedRequest();
        assigned.ifPresent(this::synchronizeAssignedRedisProjection);
        assigned.ifPresent(speakingQueue -> log.info(
                "Speaking request assigned. roomId={}, userId={}, queueOrder={}, expiresAt={}",
                speakingQueue.getRoomId(),
                speakingQueue.getUserId(),
                speakingQueue.getQueueOrder(),
                speakingQueue.getExpiresAt()
        ));
        assigned.ifPresent(speakingQueue ->
                publishStageChanged(StageEventType.SPEAKER_ASSIGNED, speakingQueue));
        assigned.ifPresent(this::suggestAiCounterIssue);
        return assigned;
    }

    public void cancelSpeakingRequest(Long roomId, Long userId) {
        SpeakingQueue canceled =
                speakingQueuePersistenceService.cancelWaitingRequest(roomId, userId);
        synchronizeCanceledRedisProjection(canceled);
        log.info(
                "Speaking request canceled. roomId={}, userId={}, queueOrder={}",
                canceled.getRoomId(),
                canceled.getUserId(),
                canceled.getQueueOrder()
        );
        publishStageChanged(StageEventType.SPEAKING_CANCELED, canceled);
        tryAssignNextSpeaker(roomId);
    }

    public StageRequestStatusRes getMySpeakingRequestStatus(Long roomId, Long userId) {
        Optional<SpeakingQueue> activeRequest =
                speakingQueuePersistenceService.findMyActiveRequest(roomId, userId);

        return activeRequest
                .map(request -> StageRequestStatusRes.from(
                        request,
                        currentWaitingRank(request)
                ))
                .orElseGet(StageRequestStatusRes::empty);
    }

    public void completeSpeakingTurn(Long roomId, Long userId) {
        SpeakingQueue completed =
                speakingQueuePersistenceService.completeCurrentSpeaker(roomId, userId);
        synchronizeCompletedRedisProjection(completed);
        log.info(
                "Speaking request completed. roomId={}, userId={}, queueOrder={}",
                completed.getRoomId(),
                completed.getUserId(),
                completed.getQueueOrder()
        );
        publishStageChanged(
                StageEventType.SPEAKER_COMPLETED,
                completed,
                StageTurnEndReason.COMPLETED
        );
        tryAssignNextSpeaker(roomId);
    }

    public void completeCurrentSpeakerWhenParticipantLeft(Long roomId, Long userId) {
        Optional<SpeakingQueue> completed =
                speakingQueuePersistenceService.completeCurrentSpeakerIfMatches(roomId, userId);
        completed.ifPresent(this::handleParticipantLeftTurnCompletion);
    }

    public void closeSpeakingQueuesWhenRoomClosed(Long roomId, LocalDateTime closedAt) {
        SpeakingQueueRoomCloseResult closeResult =
                speakingQueuePersistenceService.closeActiveRequestsByRoomId(roomId, closedAt);

        closeResult.canceledRequests().forEach(this::synchronizeCanceledRedisProjection);
        closeResult.completedRequests().forEach(this::synchronizeCompletedRedisProjection);

        if (!closeResult.canceledRequests().isEmpty()
                || !closeResult.completedRequests().isEmpty()) {
            log.info(
                    "Speaking requests closed because room was closed. "
                            + "roomId={}, canceledCount={}, completedCount={}",
                    roomId,
                    closeResult.canceledRequests().size(),
                    closeResult.completedRequests().size()
            );
        }
    }

    public StageCurrentSpeakerRes getCurrentSpeaker(Long roomId) {
        Optional<CurrentSpeakerProjection> currentSpeaker =
                speakingQueuePersistenceService.findCurrentSpeaker(roomId);

        if (currentSpeaker.isEmpty()) {
            return StageCurrentSpeakerRes.empty();
        }

        return StageCurrentSpeakerRes.from(currentSpeaker.get());
    }

    public StageQueueRes getQueueSummary(Long roomId) {
        return getWaitingQueue(
                roomId,
                0,
                speakingQueueProperties.getQueue().getSummarySize()
        );
    }

    public StageQueueRes getWaitingQueue(Long roomId, Integer offset, Integer size) {
        speakingQueuePersistenceService.validateRoomExists(roomId);

        int resolvedOffset = resolveOffset(offset);
        int resolvedSize = resolveSize(size);
        long totalWaitingCount = redisSpeakingQueueRepository.count(roomId);
        long start = resolvedOffset;
        long end = resolvedOffset + (long) resolvedSize - 1;
        List<Long> userIds =
                redisSpeakingQueueRepository.findWaitingUserIds(roomId, start, end);
        Map<Long, String> nicknames =
                speakingQueuePersistenceService.findNicknamesByUserIds(userIds);
        List<StageQueueRes.WaitingSpeaker> items =
                IntStream.range(0, userIds.size())
                        .mapToObj(index -> waitingSpeaker(
                                resolvedOffset,
                                index,
                                userIds.get(index),
                                nicknames
                        ))
                        .toList();

        return StageQueueRes.of(
                totalWaitingCount,
                resolvedOffset,
                resolvedSize,
                items
        );
    }

    public Optional<SpeakingQueue> expireCurrentSpeaker(
            Long roomId,
            LocalDateTime now
    ) {
        Optional<SpeakingQueue> expired =
                speakingQueuePersistenceService.expireCurrentSpeaker(roomId, now);
        expired.ifPresent(this::synchronizeCompletedRedisProjection);
        expired.ifPresent(speakingQueue -> log.info(
                "Speaking request expired. roomId={}, userId={}, queueOrder={}, expiredAt={}",
                speakingQueue.getRoomId(),
                speakingQueue.getUserId(),
                speakingQueue.getQueueOrder(),
                now
        ));
        expired.ifPresent(speakingQueue -> publishStageChanged(
                StageEventType.SPEAKER_EXPIRED,
                speakingQueue,
                StageTurnEndReason.EXPIRED
        ));
        expired.ifPresent(speakingQueue -> tryAssignNextSpeaker(roomId));
        return expired;
    }

    private void publishStageChanged(
            StageEventType type,
            SpeakingQueue speakingQueue
    ) {
        publishStageChanged(type, speakingQueue, null);
    }

    private void publishStageChanged(
            StageEventType type,
            SpeakingQueue speakingQueue,
            StageTurnEndReason endReason
    ) {
        eventPublisher.publishEvent(new StageChangedEvent(
                type,
                speakingQueue.getRoomId(),
                StageEventPayload.from(speakingQueue, endReason)
        ));
    }

    private void handleParticipantLeftTurnCompletion(SpeakingQueue completed) {
        log.info(
                "Speaking request completed because participant left room. "
                        + "roomId={}, userId={}, queueOrder={}",
                completed.getRoomId(),
                completed.getUserId(),
                completed.getQueueOrder()
        );

        publishStageChanged(
                StageEventType.SPEAKER_COMPLETED,
                completed,
                StageTurnEndReason.LEFT_ROOM
        );

        synchronizeParticipantLeftTurnCompletionAfterCommit(completed);
    }

    private void synchronizeParticipantLeftTurnCompletionAfterCommit(SpeakingQueue completed) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            synchronizeParticipantLeftTurnCompletion(completed);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        synchronizeParticipantLeftTurnCompletion(completed);
                    }
                }
        );
    }

    private void synchronizeParticipantLeftTurnCompletion(SpeakingQueue completed) {
        synchronizeCompletedRedisProjection(completed);
        tryAssignNextSpeaker(completed.getRoomId());
    }

    private Optional<SpeakingQueue> tryAssignNextSpeaker(Long roomId) {
        try {
            return assignNextSpeaker(roomId);
        } catch (RuntimeException assignmentException) {
            log.error(
                    "Failed to assign next speaker after stage state change. roomId={}",
                    roomId,
                    assignmentException
            );
            return Optional.empty();
        }
    }

    private Integer currentWaitingRank(SpeakingQueue speakingQueue) {
        if (speakingQueue.getStatus() != SpeakingQueueStatus.WAITING) {
            return null;
        }

        return redisSpeakingQueueRepository.rank(
                speakingQueue.getRoomId(),
                speakingQueue.getUserId()
        ).orElse(null);
    }

    private int resolveOffset(Integer offset) {
        if (offset == null) {
            return 0;
        }
        return offset;
    }

    private int resolveSize(Integer size) {
        int resolvedSize = size == null
                ? speakingQueueProperties.getQueue().getDefaultPageSize()
                : size;

        if (resolvedSize > speakingQueueProperties.getQueue().getMaxPageSize()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return resolvedSize;
    }

    private StageQueueRes.WaitingSpeaker waitingSpeaker(
            int offset,
            int index,
            Long userId,
            Map<Long, String> nicknames
    ) {
        return new StageQueueRes.WaitingSpeaker(
                offset + index + 1,
                userId,
                nicknames.get(userId)
        );
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
            rebuildRedisProjection(speakingQueue.getRoomId());
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
            rebuildRedisProjection(speakingQueue.getRoomId());
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
            rebuildRedisProjection(speakingQueue.getRoomId());
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
            rebuildRedisProjection(speakingQueue.getRoomId());
        }
    }

    private void rebuildRedisProjection(Long roomId) {
        for (int attempt = 1; attempt <= REDIS_PROJECTION_REBUILD_MAX_ATTEMPTS; attempt++) {
            RedisProjectionRebuildAttemptResult attemptResult =
                    rebuildRedisProjectionOnce(roomId, attempt);
            if (attemptResult.isFinished()) {
                return;
            }
            if (!sleepBeforeRedisProjectionRebuildRetry(roomId, attempt)) {
                return;
            }
        }
    }

    private RedisProjectionRebuildAttemptResult rebuildRedisProjectionOnce(
            Long roomId,
            int attempt
    ) {
        Optional<Long> expectedVersion = currentProjectionVersionForRebuild(roomId, attempt);
        if (expectedVersion.isEmpty()) {
            return retryOrStopRedisProjectionRebuild(attempt);
        }

        Optional<RedisProjectionSource> projectionSource =
                projectionSourceForRebuild(roomId, attempt);
        if (projectionSource.isEmpty()) {
            return retryOrStopRedisProjectionRebuild(attempt);
        }

        return replaceRedisProjectionForRebuild(
                roomId,
                projectionSource.get(),
                expectedVersion.get(),
                attempt
        );
    }

    private Optional<Long> currentProjectionVersionForRebuild(Long roomId, int attempt) {
        try {
            return Optional.of(redisSpeakingQueueRepository.currentProjectionVersion(roomId));
        } catch (RuntimeException versionException) {
            if (attempt == REDIS_PROJECTION_REBUILD_MAX_ATTEMPTS) {
                log.error(
                        "Failed to read speaking Redis projection version. "
                                + "roomId={}, attempts={}",
                        roomId,
                        attempt,
                        versionException
                );
                return Optional.empty();
            }
            log.warn(
                    "Retrying speaking Redis projection version read. "
                            + "roomId={}, attempt={}",
                    roomId,
                    attempt,
                    versionException
            );
            return Optional.empty();
        }
    }

    private Optional<RedisProjectionSource> projectionSourceForRebuild(
            Long roomId,
            int attempt
    ) {
        try {
            return Optional.of(new RedisProjectionSource(
                    speakingQueuePersistenceService.findWaitingRequestsForRedisProjection(roomId),
                    speakingQueuePersistenceService.findCurrentSpeakerForRedisProjection(roomId)
            ));
        } catch (RuntimeException projectionSourceException) {
            if (attempt == REDIS_PROJECTION_REBUILD_MAX_ATTEMPTS) {
                log.error(
                        "Failed to load speaking Redis projection source. "
                                + "roomId={}, attempts={}",
                        roomId,
                        attempt,
                        projectionSourceException
                );
                return Optional.empty();
            }
            log.warn(
                    "Retrying speaking Redis projection source load. roomId={}, attempt={}",
                    roomId,
                    attempt,
                    projectionSourceException
            );
            return Optional.empty();
        }
    }

    private RedisProjectionRebuildAttemptResult replaceRedisProjectionForRebuild(
            Long roomId,
            RedisProjectionSource projectionSource,
            long expectedVersion,
            int attempt
    ) {
        try {
            boolean replaced =
                    redisSpeakingQueueRepository.replaceRoomProjectionIfVersionMatches(
                            roomId,
                            projectionSource.waitingQueues(),
                            projectionSource.currentSpeaker(),
                            expectedVersion
                    );
            if (!replaced) {
                log.warn(
                        "Skipped stale speaking Redis projection rebuild. "
                                + "roomId={}, attempt={}, expectedVersion={}",
                        roomId,
                        attempt,
                        expectedVersion
                );
                return RedisProjectionRebuildAttemptResult.RETRY;
            }
            log.info(
                    "Speaking Redis projection rebuilt. "
                            + "roomId={}, attempt={}, expectedVersion={}",
                    roomId,
                    attempt,
                    expectedVersion
            );
            return RedisProjectionRebuildAttemptResult.SUCCESS;
        } catch (RuntimeException rebuildException) {
            if (attempt == REDIS_PROJECTION_REBUILD_MAX_ATTEMPTS) {
                log.error(
                        "Failed to rebuild speaking Redis projection. roomId={}, attempts={}",
                        roomId,
                        attempt,
                        rebuildException
                );
                return RedisProjectionRebuildAttemptResult.STOP;
            }
            log.warn(
                    "Retrying speaking Redis projection rebuild. roomId={}, attempt={}",
                    roomId,
                    attempt,
                    rebuildException
            );
            return RedisProjectionRebuildAttemptResult.RETRY;
        }
    }

    private RedisProjectionRebuildAttemptResult retryOrStopRedisProjectionRebuild(int attempt) {
        if (attempt == REDIS_PROJECTION_REBUILD_MAX_ATTEMPTS) {
            return RedisProjectionRebuildAttemptResult.STOP;
        }
        return RedisProjectionRebuildAttemptResult.RETRY;
    }

    private boolean sleepBeforeRedisProjectionRebuildRetry(Long roomId, int attempt) {
        if (attempt >= REDIS_PROJECTION_REBUILD_MAX_ATTEMPTS) {
            return true;
        }

        long delayMillis = REDIS_PROJECTION_REBUILD_INITIAL_BACKOFF_MS;
        for (int index = 1; index < attempt; index++) {
            delayMillis *= REDIS_PROJECTION_REBUILD_BACKOFF_MULTIPLIER;
        }

        try {
            Thread.sleep(delayMillis);
            return true;
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            log.warn(
                    "Interrupted while waiting to retry speaking Redis projection rebuild. "
                            + "roomId={}, attempt={}, delayMillis={}",
                    roomId,
                    attempt,
                    delayMillis,
                    interruptedException
            );
            return false;
        }
    }

    private enum RedisProjectionRebuildAttemptResult {
        SUCCESS,
        RETRY,
        STOP;

        private boolean isFinished() {
            return this != RETRY;
        }
    }

    private record RedisProjectionSource(
            List<SpeakingQueue> waitingQueues,
            Optional<SpeakingQueue> currentSpeaker
    ) {
    }

    private void suggestAiCounterIssue(SpeakingQueue speakingQueue) {
        try {
            aiCounterIssueService.suggestIfNeeded(speakingQueue.getRoomId());
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to suggest AI counter issue. roomId={}, queueId={}",
                    speakingQueue.getRoomId(),
                    speakingQueue.getId(),
                    exception
            );
        }
    }

}
