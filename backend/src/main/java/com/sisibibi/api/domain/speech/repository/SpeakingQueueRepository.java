package com.sisibibi.api.domain.speech.repository;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeakingQueueRepository extends JpaRepository<SpeakingQueue, Long> {

    boolean existsByRoomIdAndUserIdAndStatusIn(
            Long roomId,
            Long userId,
            Collection<SpeakingQueueStatus> statuses
    );
}
