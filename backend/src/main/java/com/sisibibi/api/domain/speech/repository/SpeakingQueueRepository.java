package com.sisibibi.api.domain.speech.repository;

import com.sisibibi.api.domain.speech.entity.SpeakingQueue;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.projection.CurrentSpeakerProjection;
import java.time.Duration;
import java.time.LocalDateTime;
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

    List<SpeakingQueue> findByRoomIdAndStatusOrderByQueueOrderAsc(
            Long roomId,
            SpeakingQueueStatus status
    );

    @Query(value = """
            select *
            from speaking_queue
            where room_id = :roomId
              and status = :status
            order by queue_order asc
            limit :size
            offset :offset
            """, nativeQuery = true)
    List<SpeakingQueue> findWaitingPageForRedisReadFallback(
            @Param("roomId") Long roomId,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("size") int size
    );

    long countByRoomIdAndStatus(
            Long roomId,
            SpeakingQueueStatus status
    );

    long countByRoomIdAndStatusAndQueueOrderLessThan(
            Long roomId,
            SpeakingQueueStatus status,
            Integer queueOrder
    );

    List<SpeakingQueue> findByRoomIdAndStatusInOrderByQueueOrderAsc(
            Long roomId,
            Collection<SpeakingQueueStatus> statuses
    );

    Optional<SpeakingQueue> findFirstByRoomIdAndStatusAndStanceOrderByQueueOrderAsc(
            Long roomId,
            SpeakingQueueStatus status,
            SpeechStance stance
    );

    List<SpeakingQueue> findTop3ByRoomIdAndStatusInAndStanceIsNotNullOrderByAssignedAtDesc(
            Long roomId,
            Collection<SpeakingQueueStatus> statuses
    );

    Optional<SpeakingQueue> findByRoomIdAndStatus(
            Long roomId,
            SpeakingQueueStatus status
    );

    @Query("""
            select queue.userId as userId,
                   speaker.nickname as nickname,
                   queue.stance as stance,
                   queue.queueOrder as queueOrder,
                   queue.assignedAt as assignedAt,
                   queue.expiresAt as expiresAt
            from SpeakingQueue queue
            join User speaker on queue.userId = speaker.id
            where queue.roomId = :roomId
              and queue.status = :status
            """)
    Optional<CurrentSpeakerProjection> findCurrentSpeakerProjection(
            @Param("roomId") Long roomId,
            @Param("status") SpeakingQueueStatus status
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

    default List<Long> findRoomIdsWithExpiredSpeaker(LocalDateTime now) {
        return findRoomIdsWithExpiredSpeaker(
                SpeakingQueueStatus.ASSIGNED,
                now
        );
    }

    @Query("""
            select distinct queue.roomId
            from SpeakingQueue queue
            where queue.status = :assignedStatus
              and queue.expiresAt <= :now
            """)
    List<Long> findRoomIdsWithExpiredSpeaker(
            @Param("assignedStatus") SpeakingQueueStatus assignedStatus,
            @Param("now") LocalDateTime now
    );

    default List<Long> findRoomIdsRequiringIdleWarning(
            LocalDateTime now,
            Duration warningDelay,
            Duration warningSuppressionBeforeExpiration
    ) {
        return findRoomIdsRequiringIdleWarning(
                SpeakingQueueStatus.ASSIGNED,
                now.minus(warningDelay),
                now.plus(warningSuppressionBeforeExpiration)
        );
    }

    @Query("""
            select distinct queue.roomId
            from SpeakingQueue queue
            where queue.status = :assignedStatus
              and queue.idleWarningSent = false
              and queue.lastActivityAt is not null
              and queue.lastActivityAt <= :activityCutoff
              and queue.expiresAt > :expirationWarningCutoff
            """)
    List<Long> findRoomIdsRequiringIdleWarning(
            @Param("assignedStatus") SpeakingQueueStatus assignedStatus,
            @Param("activityCutoff") LocalDateTime activityCutoff,
            @Param("expirationWarningCutoff") LocalDateTime expirationWarningCutoff
    );

    default List<Long> findRoomIdsWithIdleTimedOutSpeaker(
            LocalDateTime now,
            Duration timeoutDelayAfterWarning
    ) {
        return findRoomIdsWithIdleTimedOutSpeaker(
                SpeakingQueueStatus.ASSIGNED,
                now.minus(timeoutDelayAfterWarning),
                now
        );
    }

    @Query("""
            select distinct queue.roomId
            from SpeakingQueue queue
            where queue.status = :assignedStatus
              and queue.idleWarningSent = true
              and queue.idleWarnedAt is not null
              and queue.idleWarnedAt <= :warningCutoff
              and queue.expiresAt > :now
            """)
    List<Long> findRoomIdsWithIdleTimedOutSpeaker(
            @Param("assignedStatus") SpeakingQueueStatus assignedStatus,
            @Param("warningCutoff") LocalDateTime warningCutoff,
            @Param("now") LocalDateTime now
    );
}
