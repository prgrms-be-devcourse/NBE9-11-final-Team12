package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.speech.config.StageSummaryProperties;
import com.sisibibi.api.domain.speech.dto.event.StageSummaryChangedEvent;
import com.sisibibi.api.domain.speech.dto.event.StageSummaryEventPayload;
import com.sisibibi.api.domain.speech.dto.event.StageSummaryEventType;
import com.sisibibi.api.domain.speech.entity.StageSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class StageSummaryService {

    private final StageSummaryPersistenceService stageSummaryPersistenceService;
    private final StageSummaryGenerator stageSummaryGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final Executor stageSummaryExecutor;
    private final StageSummaryProperties stageSummaryProperties;

    public StageSummaryService(
            StageSummaryPersistenceService stageSummaryPersistenceService,
            StageSummaryGenerator stageSummaryGenerator,
            ApplicationEventPublisher eventPublisher,
            @Qualifier("stageSummaryTaskExecutor") Executor stageSummaryExecutor,
            StageSummaryProperties stageSummaryProperties
    ) {
        this.stageSummaryPersistenceService = stageSummaryPersistenceService;
        this.stageSummaryGenerator = stageSummaryGenerator;
        this.eventPublisher = eventPublisher;
        this.stageSummaryExecutor = stageSummaryExecutor;
        this.stageSummaryProperties = stageSummaryProperties;
    }

    public void generateIfNeeded(Long roomId) {
        if (!stageSummaryProperties.isEnabled()) {
            return;
        }

        StageSummaryGenerationContext context =
                stageSummaryPersistenceService.prepareGeneration(
                        roomId,
                        LocalDateTime.now(),
                        stageSummaryProperties.getMinCompletedSpeakerCount(),
                        stageSummaryProperties.getMaxGenerationAttempts(),
                        stageSummaryProperties.getGenerateTimeout()
                );

        if (!context.shouldCallAi()) {
            return;
        }

        try {
            StageSummaryResult result = generateWithTimeout(context);
            validateResult(result);
            StageSummary completed = stageSummaryPersistenceService.complete(
                    context.summaryId(),
                    result,
                    context.speeches().size(),
                    LocalDateTime.now()
            );
            publishStageSummaryCompleted(completed);
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to generate stage summary. roomId={}, summaryId={}",
                    roomId,
                    context.summaryId(),
                    exception
            );
            stageSummaryPersistenceService.fail(
                    context.summaryId(),
                    exception.getMessage() == null
                            ? "Stage summary generation failed."
                            : exception.getMessage()
            );
        }
    }

    private StageSummaryResult generateWithTimeout(StageSummaryGenerationContext context) {
        Duration timeout = stageSummaryProperties.getGenerateTimeout();
        long timeoutMillis = Math.max(1L, timeout.toMillis());

        CompletableFuture<StageSummaryResult> generation =
                CompletableFuture.supplyAsync(
                        () -> stageSummaryGenerator.generate(context.room(), context.speeches()),
                        stageSummaryExecutor
                );

        try {
            return generation.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            cancelGeneration(generation);
            throw new IllegalStateException(
                    "Stage summary generation timed out after " + timeoutMillis + "ms.",
                    exception
            );
        } catch (InterruptedException exception) {
            cancelGeneration(generation);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Stage summary generation was interrupted.", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Stage summary generation failed.", cause);
        }
    }

    private void cancelGeneration(CompletableFuture<StageSummaryResult> generation) {
        generation.cancel(true);
    }

    private void validateResult(StageSummaryResult result) {
        if (result == null || result.moderatorSummary() == null
                || result.moderatorSummary().isBlank()) {
            throw new IllegalStateException("Stage summary moderator summary is blank.");
        }

        List<String> keyPoints = result.keyPoints();
        if (keyPoints == null || keyPoints.size() != 3
                || keyPoints.stream().anyMatch(point -> point == null || point.isBlank())) {
            throw new IllegalStateException("Stage summary must have exactly three key points.");
        }
    }

    private void publishStageSummaryCompleted(StageSummary summary) {
        eventPublisher.publishEvent(new StageSummaryChangedEvent(
                StageSummaryEventType.STAGE_SUMMARY_COMPLETED,
                summary.getRoomId(),
                StageSummaryEventPayload.from(summary)
        ));
    }
}
