package com.sisibibi.api.domain.speech.repository;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface SpeakingQueueRepository extends JpaRepository<SpeakingQueue, Long> {

    boolean existsByRoomIdAndUserIdAndStatusIn(
            Long roomId,
            Long userId,
            Collection<SpeakingQueueStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SpeakingQueue> findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
            Long roomId,
            SpeakingQueueStatus status
    );

    boolean existsByRoomIdAndStatus(
            Long roomId,
            SpeakingQueueStatus status
    );
}
