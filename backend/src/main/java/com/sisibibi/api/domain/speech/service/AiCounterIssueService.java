package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.config.AiCounterIssueProperties;
import com.sisibibi.api.domain.speech.config.SpeechAiGenerator;
import com.sisibibi.api.domain.speech.dto.event.AiCounterIssueChangedEvent;
import com.sisibibi.api.domain.speech.dto.event.AiCounterIssueEventPayload;
import com.sisibibi.api.domain.speech.dto.event.AiCounterIssueEventType;
import com.sisibibi.api.domain.speech.entity.AiCounterIssue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.util.SpeakingStreakPolicy;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiCounterIssueService {

    private static final List<SpeakingQueueStatus> STREAK_TARGET_STATUSES =
            List.of(SpeakingQueueStatus.COMPLETED);

    private final SpeakingQueueRepository speakingQueueRepository;
    private final AiCounterIssuePersistenceService aiCounterIssuePersistenceService;
    private final RoomRepository roomRepository;
    private final SpeakingStreakPolicy speakingStreakPolicy;
    private final SpeechAiGenerator aiCounterIssueGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final AiCounterIssueProperties aiCounterIssueProperties;

    public AiCounterIssueService(
            SpeakingQueueRepository speakingQueueRepository,
            AiCounterIssuePersistenceService aiCounterIssuePersistenceService,
            RoomRepository roomRepository,
            SpeakingStreakPolicy speakingStreakPolicy,
            SpeechAiGenerator aiCounterIssueGenerator,
            ApplicationEventPublisher eventPublisher,
            AiCounterIssueProperties aiCounterIssueProperties
    ) {
        this.speakingQueueRepository = speakingQueueRepository;
        this.aiCounterIssuePersistenceService = aiCounterIssuePersistenceService;
        this.roomRepository = roomRepository;
        this.speakingStreakPolicy = speakingStreakPolicy;
        this.aiCounterIssueGenerator = aiCounterIssueGenerator;
        this.eventPublisher = eventPublisher;
        this.aiCounterIssueProperties = aiCounterIssueProperties;
    }

    public void suggestIfNeeded(Long roomId) {
        List<SpeakingQueue> recentAssignments =
                speakingQueueRepository
                        .findTop3ByRoomIdAndStatusInAndStanceIsNotNullOrderByAssignedAtDesc(
                                roomId,
                                STREAK_TARGET_STATUSES
                        );

        Optional<SpeechStance> targetStance =
                speakingStreakPolicy.counterStanceFor(recentAssignments);

        if (targetStance.isEmpty()) {
            return;
        }

        Long triggerQueueId = recentAssignments.getFirst().getId();

        Optional<AiCounterIssue> pendingIssue =
                aiCounterIssuePersistenceService.createPendingIfAbsent(
                        roomId,
                        triggerQueueId,
                        targetStance.get()
                );

        pendingIssue.ifPresent(this::generateAndComplete);
    }

    private void generateAndComplete(AiCounterIssue issue) {
        try {
            Room room = roomRepository.findById(issue.getRoomId())
                    .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

            generateAndCompleteWithRetries(issue, room);
        } catch (RuntimeException exception) {
            failIssue(issue, exception);
        }
    }

    private void generateAndCompleteWithRetries(AiCounterIssue issue, Room room) {
        int maxAttempts = Math.max(1, aiCounterIssueProperties.getMaxGenerationAttempts());
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            AiCounterIssue attemptedIssue =
                    aiCounterIssuePersistenceService.markAttemptStarted(issue.getId());

            try {
                String content =
                        aiCounterIssueGenerator.generate(room, attemptedIssue.getTargetStance());
                AiCounterIssue completedIssue =
                        aiCounterIssuePersistenceService.complete(attemptedIssue.getId(), content);
                publishAiCounterIssueChangedEvent(completedIssue);
                return;
            } catch (RuntimeException exception) {
                lastException = exception;
                log.warn(
                        "AI counter issue generation attempt failed. "
                                + "roomId={}, triggerQueueId={}, attempt={}, maxAttempts={}",
                        attemptedIssue.getRoomId(),
                        attemptedIssue.getTriggerQueueId(),
                        attempt,
                        maxAttempts,
                        exception
                );
            }
        }

        failIssue(issue, lastException != null
                ? lastException
                : new IllegalStateException("Unknown AI counter issue generation failure."));
    }

    private void failIssue(AiCounterIssue issue, RuntimeException exception) {
        RuntimeException failure = exception != null
                ? exception
                : new IllegalStateException("Unknown AI counter issue generation failure.");
        log.warn(
                "Failed to generate AI counter issue. roomId={}, triggerQueueId={}",
                issue.getRoomId(),
                issue.getTriggerQueueId(),
                failure
        );
        aiCounterIssuePersistenceService.fail(issue.getId(), failure.getMessage());
    }

    private void publishAiCounterIssueChangedEvent(AiCounterIssue issue) {
        eventPublisher.publishEvent(new AiCounterIssueChangedEvent(
                AiCounterIssueEventType.AI_COUNTER_ISSUE_SUGGESTED,
                issue.getRoomId(),
                AiCounterIssueEventPayload.from(issue)
        ));
    }
}
