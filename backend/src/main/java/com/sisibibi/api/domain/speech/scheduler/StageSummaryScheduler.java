package com.sisibibi.api.domain.speech.scheduler;

import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.config.StageSummaryProperties;
import com.sisibibi.api.domain.speech.entity.StageSummary;
import com.sisibibi.api.domain.speech.entity.StageSummaryStatus;
import com.sisibibi.api.domain.speech.repository.StageSummaryRepository;
import com.sisibibi.api.domain.speech.service.StageSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class StageSummaryScheduler {

    private final RoomRepository roomRepository;
    private final StageSummaryRepository stageSummaryRepository;
    private final StageSummaryService stageSummaryService;
    private final StageSummaryProperties stageSummaryProperties;

    @Scheduled(fixedDelayString = "${app.stage-summary.recovery-fixed-delay-ms:60000}")
    public void recoverMissedSummariesAndRetries() {
        if (!stageSummaryProperties.isEnabled()) {
            return;
        }

        int batchSize = Math.max(1, stageSummaryProperties.getRecoveryBatchSize());
        PageRequest page = PageRequest.of(0, batchSize);
        LocalDateTime now = LocalDateTime.now();

        roomRepository.findStageSummaryCandidateRoomIds(now, page)
                .forEach(this::generateQuietly);

        stageSummaryRepository.findRetryCandidates(
                        StageSummaryStatus.FAILED,
                        stageSummaryProperties.getMaxGenerationAttempts(),
                        page
                )
                .stream()
                .map(StageSummary::getRoomId)
                .forEach(this::generateQuietly);
    }

    private void generateQuietly(Long roomId) {
        try {
            stageSummaryService.generateIfNeeded(roomId);
        } catch (RuntimeException exception) {
            log.warn("Failed to recover stage summary. roomId={}", roomId, exception);
        }
    }
}
