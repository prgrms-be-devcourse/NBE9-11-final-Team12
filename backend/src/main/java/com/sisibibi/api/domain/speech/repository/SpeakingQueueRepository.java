package com.sisibibi.api.domain.speech.repository;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpeakingQueueRepository extends JpaRepository<SpeakingQueue, Long> {

    boolean existsByRoomIdAndUserIdAndStatusIn(
            Long roomId,
            Long userId,
            Collection<SpeakingQueueStatus> statuses
    );

    Optional<SpeakingQueue> findByRoomIdAndUserIdAndStatusIn(
            Long roomId,
            Long userId,
            Collection<SpeakingQueueStatus> statuses
    );

    Optional<SpeakingQueue> findFirstByRoomIdAndStatusOrderByQueueOrderAsc(
            Long roomId,
            SpeakingQueueStatus status
    );

    boolean existsByRoomIdAndStatus(
            Long roomId,
            SpeakingQueueStatus status
    );

    default List<Long> findRoomIdsRequiringAssignment() {
        return findRoomIdsRequiringAssignment(
                SpeakingQueueStatus.WAITING,
                SpeakingQueueStatus.ASSIGNED
        );
    }

    @Query("""
            select queue.roomId
            from SpeakingQueue queue
            group by queue.roomId
            having sum(
                case when queue.status = :waitingStatus then 1 else 0 end
            ) > 0
            and sum(
                case when queue.status = :assignedStatus then 1 else 0 end
            ) = 0
            """)
    List<Long> findRoomIdsRequiringAssignment(
            @Param("waitingStatus") SpeakingQueueStatus waitingStatus,
            @Param("assignedStatus") SpeakingQueueStatus assignedStatus
    );
}
