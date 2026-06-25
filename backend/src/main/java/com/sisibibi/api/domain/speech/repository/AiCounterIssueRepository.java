package com.sisibibi.api.domain.speech.repository;

import com.sisibibi.api.domain.speech.entity.AiCounterIssue;
import com.sisibibi.api.domain.speech.entity.AiCounterIssueStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiCounterIssueRepository extends JpaRepository<AiCounterIssue, Long> {

    boolean existsByRoomIdAndTriggerQueueId(Long roomId, Long triggerQueueId);

    List<AiCounterIssue> findTop10ByRoomIdAndStatusOrderByCreatedAtDesc(
            Long roomId,
            AiCounterIssueStatus status
    );
}
