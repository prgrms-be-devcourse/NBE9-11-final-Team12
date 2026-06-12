package com.sisibibi.api.domain.speech.repository;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpeakingQueueRepository extends JpaRepository<SpeakingQueue, Long> {

    boolean existsByRoomIdAndUserIdAndStatusIn(
            Long roomId,
            Long userId,
            Collection<SpeakingQueueStatus> statuses
    );

    Optional<SpeakingQueue> findFirstByRoomIdAndUserIdAndStatusOrderByQueueOrderAsc(
            Long roomId,
            Long userId,
            SpeakingQueueStatus status
    );

    Optional<SpeakingQueue> findFirstByRoomIdAndUserIdOrderByIdDesc(Long roomId, Long userId);

    List<SpeakingQueue> findByRoomIdAndStatusOrderByQueueOrderAsc(
            Long roomId,
            SpeakingQueueStatus status
    );

    @Query("select coalesce(max(s.queueOrder), 0) from SpeakingQueue s where s.roomId = :roomId")
    int findMaxQueueOrderByRoomId(Long roomId);
}
