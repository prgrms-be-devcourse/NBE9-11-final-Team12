package com.sisibibi.api.domain.speech.service;

import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.entity.AiCounterIssue;
import com.sisibibi.api.domain.speech.entity.AiCounterIssueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.AiCounterIssueRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiCounterIssuePersistenceService {

    private final AiCounterIssueRepository aiCounterIssueRepository;
    private final RoomRepository roomRepository;

    @Transactional
    public Optional<AiCounterIssue> createPendingIfAbsent(
            Long roomId,
            Long triggerQueueId,
            SpeechStance targetStance
    ) {
        if (aiCounterIssueRepository.existsByRoomIdAndTriggerQueueId(roomId, triggerQueueId)) {
            return Optional.empty();
        }

        try {
            AiCounterIssue issue = AiCounterIssue.pending(roomId, triggerQueueId, targetStance);
            return Optional.of(aiCounterIssueRepository.saveAndFlush(issue));
        } catch (DataIntegrityViolationException duplicateTrigger) {
            return Optional.empty();
        }
    }

    @Transactional
    public AiCounterIssue complete(Long issueId, String content) {
        AiCounterIssue issue = findIssue(issueId);
        issue.complete(content, LocalDateTime.now());
        return issue;
    }

    @Transactional
    public void fail(Long issueId, String errorMessage) {
        AiCounterIssue issue = findIssue(issueId);
        issue.fail(errorMessage == null ? "AI counter issue generation failed." : errorMessage);
    }

    @Transactional(readOnly = true)
    public List<AiCounterIssue> findRecentCompleted(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
        }

        return aiCounterIssueRepository.findTop10ByRoomIdAndStatusOrderByCreatedAtDesc(
                roomId,
                AiCounterIssueStatus.COMPLETED
        );
    }

    private AiCounterIssue findIssue(Long issueId) {
        return aiCounterIssueRepository.findById(issueId)
                .orElseThrow(() -> new IllegalStateException("AI counter issue not found."));
    }
}
