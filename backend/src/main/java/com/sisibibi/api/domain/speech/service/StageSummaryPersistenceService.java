package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.StageSummary;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speech.repository.StageSummaryRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StageSummaryPersistenceService {

    private final RoomRepository roomRepository;
    private final SpeakingQueueRepository speakingQueueRepository;
    private final SpeechRepository speechRepository;
    private final StageSummaryRepository stageSummaryRepository;

    @Transactional
    public StageSummaryGenerationContext prepareGeneration(
            Long roomId,
            LocalDateTime now,
            int minCompletedSpeakerCount,
            int maxAttempts
    ) {
        Room room = roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!isAtOrAfterMidpoint(room, now)) {
            return StageSummaryGenerationContext.skip();
        }

        long completedSpeakerCount =
                speakingQueueRepository.countDistinctCompletedSpeakersByRoomId(roomId);
        if (completedSpeakerCount < minCompletedSpeakerCount) {
            return StageSummaryGenerationContext.skip();
        }

        StageSummary summary = stageSummaryRepository.findByRoomIdForUpdate(roomId)
                .orElse(null);
        if (summary != null) {
            if (summary.shouldSkipGeneration() || !summary.canRetry(maxAttempts)) {
                return StageSummaryGenerationContext.skip();
            }

            summary.markPendingForRetry();
        } else {
            summary = stageSummaryRepository.saveAndFlush(StageSummary.pending(
                    roomId,
                    now,
                    Math.toIntExact(completedSpeakerCount)
            ));
        }

        summary.markAttemptStarted(now);
        List<Speech> speeches =
                speechRepository.findStageSummarySourceSpeeches(roomId, summary.getTriggeredAt());

        return StageSummaryGenerationContext.callAi(summary.getId(), room, speeches);
    }

    @Transactional
    public StageSummary complete(
            Long summaryId,
            StageSummaryResult result,
            int speechCount,
            LocalDateTime completedAt
    ) {
        StageSummary summary = findSummary(summaryId);
        summary.complete(
                result.moderatorSummary(),
                result.keyPoints(),
                speechCount,
                completedAt
        );
        return summary;
    }

    @Transactional
    public StageSummary fail(Long summaryId, String errorMessage) {
        StageSummary summary = findSummary(summaryId);
        summary.fail(errorMessage);
        return summary;
    }

    public StageSummary getSummary(Long roomId) {
        return stageSummaryRepository.findByRoomId(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.STAGE_SUMMARY_NOT_FOUND));
    }

    private boolean isAtOrAfterMidpoint(Room room, LocalDateTime now) {
        if (room.getStartedAt() == null || room.getEndedAt() == null || now == null) {
            log.warn(
                    "Skipped stage summary because room time is incomplete. "
                            + "roomId={}, startedAt={}, endedAt={}, now={}",
                    room.getId(),
                    room.getStartedAt(),
                    room.getEndedAt(),
                    now
            );
            return false;
        }

        Duration duration = Duration.between(room.getStartedAt(), room.getEndedAt());
        if (duration.isNegative() || duration.isZero()) {
            log.warn(
                    "Skipped stage summary because room duration is invalid. "
                            + "roomId={}, startedAt={}, endedAt={}",
                    room.getId(),
                    room.getStartedAt(),
                    room.getEndedAt()
            );
            return false;
        }

        LocalDateTime midpoint = room.getStartedAt().plus(duration.dividedBy(2));
        return !now.isBefore(midpoint);
    }

    private StageSummary findSummary(Long summaryId) {
        return stageSummaryRepository.findById(summaryId)
                .orElseThrow(() -> new IllegalStateException("Stage summary not found."));
    }
}
