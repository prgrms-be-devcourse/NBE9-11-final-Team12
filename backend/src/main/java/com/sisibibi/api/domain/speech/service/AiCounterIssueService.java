package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.config.SpeechAiGenerator;
import com.sisibibi.api.domain.speech.entity.*;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.util.SpeakingStreakPolicy;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCounterIssueService {

    private static final List<SpeakingQueueStatus> STREAK_TARGET_STATUSES =
            List.of(SpeakingQueueStatus.ASSIGNED, SpeakingQueueStatus.COMPLETED);

    private final SpeakingQueueRepository speakingQueueRepository;
    private final AiCounterIssuePersistenceService aiCounterIssuePersistenceService;
    private final RoomRepository roomRepository;
    private final SpeakingStreakPolicy speakingStreakPolicy;
    private final SpeechAiGenerator aiCounterIssueGenerator;

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

            String content = aiCounterIssueGenerator.generate(room, issue.getTargetStance());
            aiCounterIssuePersistenceService.complete(issue.getId(), content);
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to generate AI counter issue. roomId={}, triggerQueueId={}",
                    issue.getRoomId(),
                    issue.getTriggerQueueId(),
                    exception
            );
            aiCounterIssuePersistenceService.fail(issue.getId(), exception.getMessage());
        }
    }
}
