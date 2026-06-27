package com.sisibibi.api.domain.speechreport.service;

import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipantStatus;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechDeleteReason;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speechreport.config.OffTopicAiReviewProperties;
import com.sisibibi.api.domain.speechreport.entity.OffTopicAiReview;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.repository.OffTopicAiReviewRepository;
import com.sisibibi.api.domain.speechreport.repository.SpeechReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class OffTopicAiReviewService {

    private static final double AUTO_DELETE_RELEVANCE_THRESHOLD = 0.3;

    private final SpeechReportRepository speechReportRepository;
    private final OffTopicAiReviewRepository offTopicAiReviewRepository;
    private final RoomParticipantRepository roomParticipantRepository;
    private final RoomRepository roomRepository;
    private final SpeechRepository speechRepository;
    private final OffTopicAiReviewer offTopicAiReviewer;
    private final Executor offTopicAiReviewExecutor;
    private final OffTopicAiReviewProperties properties;

    public OffTopicAiReviewService(
            SpeechReportRepository speechReportRepository,
            OffTopicAiReviewRepository offTopicAiReviewRepository,
            RoomParticipantRepository roomParticipantRepository,
            RoomRepository roomRepository,
            SpeechRepository speechRepository,
            OffTopicAiReviewer offTopicAiReviewer,
            @Qualifier("offTopicAiReviewTaskExecutor") Executor offTopicAiReviewExecutor,
            OffTopicAiReviewProperties properties
    ) {
        this.speechReportRepository = speechReportRepository;
        this.offTopicAiReviewRepository = offTopicAiReviewRepository;
        this.roomParticipantRepository = roomParticipantRepository;
        this.roomRepository = roomRepository;
        this.speechRepository = speechRepository;
        this.offTopicAiReviewer = offTopicAiReviewer;
        this.offTopicAiReviewExecutor = offTopicAiReviewExecutor;
        this.properties = properties;
    }

    public int calculateThreshold(int joinedParticipantCount) {
        return Math.max(properties.getMinReportThreshold(), joinedParticipantCount / 10);
    }

    public void triggerIfNeeded(Speech speech, SpeechReportReason reason) {
        if (!properties.isEnabled() || reason != SpeechReportReason.OFF_TOPIC) {
            return;
        }

        int participantCount = roomParticipantRepository.countByRoomIdAndStatus(
                speech.getRoomId(),
                RoomParticipantStatus.JOINED
        );
        int threshold = calculateThreshold(participantCount);
        int reportCount = speechReportRepository.countBySpeechIdAndReason(
                speech.getId(),
                SpeechReportReason.OFF_TOPIC
        );

        if (reportCount < threshold
                || offTopicAiReviewRepository.findBySpeechId(speech.getId()).isPresent()) {
            return;
        }

        OffTopicAiReview review = offTopicAiReviewRepository.save(OffTopicAiReview.pending(
                speech.getId(),
                speech.getRoomId(),
                speech.getContent(),
                reportCount,
                threshold,
                participantCount
        ));
        String roomTitle = findRoomTitle(speech.getRoomId());

        runAfterCommit(() -> CompletableFuture.runAsync(
                () -> completeReview(review, speech, roomTitle),
                offTopicAiReviewExecutor
        ));
    }

    private void runAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        task.run();
                    }
                }
        );
    }

    private void completeReview(OffTopicAiReview review, Speech speech, String roomTitle) {
        try {
            OffTopicAiReviewResult result = offTopicAiReviewer.review(speech, roomTitle);
            validateResult(result);
            review.complete(result, LocalDateTime.now());
            offTopicAiReviewRepository.save(review);
            softDeleteSpeechWhenClearlyOffTopic(review, result);
        } catch (RuntimeException exception) {
            log.warn(
                    "Off-topic AI review failed. reviewId={}, speechId={}",
                    review.getId(),
                    review.getSpeechId(),
                    exception
            );
            review.fail(exception.getMessage() == null
                    ? "Off-topic AI review failed."
                    : exception.getMessage());
            offTopicAiReviewRepository.save(review);
        }
    }

    private void softDeleteSpeechWhenClearlyOffTopic(
            OffTopicAiReview review,
            OffTopicAiReviewResult result
    ) {
        if (result.confidence() >= AUTO_DELETE_RELEVANCE_THRESHOLD) {
            return;
        }

        speechRepository.findByIdAndDeletedFalse(review.getSpeechId())
                .ifPresent(speech -> {
                    speech.softDeleteByModerator(
                            SpeechDeleteReason.OFF_TOPIC,
                            LocalDateTime.now()
                    );
                    speechRepository.save(speech);
                });
    }

    private String findRoomTitle(Long roomId) {
        return roomRepository.findById(roomId)
                .map(room -> room.getTitle() == null ? "" : room.getTitle())
                .orElse("");
    }

    private void validateResult(OffTopicAiReviewResult result) {
        if (result == null) {
            throw new IllegalStateException("Off-topic AI review result is blank.");
        }
        if (result.reason() == null || result.reason().isBlank()) {
            throw new IllegalStateException("Off-topic AI review reason is blank.");
        }
        if (result.confidence() < 0.0 || result.confidence() > 1.0) {
            throw new IllegalStateException("Off-topic AI review confidence must be between 0 and 1.");
        }
    }
}
