package com.sisibibi.api.domain.speech.repository;

import com.sisibibi.api.domain.speech.entity.RoomQueueSequence;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomQueueSequenceRepository extends JpaRepository<RoomQueueSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select queueSequence
            from RoomQueueSequence queueSequence
            where queueSequence.roomId = :roomId
            """)
    Optional<RoomQueueSequence> findByRoomIdForUpdate(@Param("roomId") Long roomId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            update room_queue_sequences
            set next_queue_order = next_queue_order + 1,
                updated_at = :issuedAt
            where room_id = :roomId
              and exists (
                select 1
                from rooms room
                where room.id = :roomId
                  and room.status = 'OPEN'
                  and (room.ended_at is null or room.ended_at > :issuedAt)
              )
            """, nativeQuery = true)
    int issueNextQueueOrderIfRoomActive(
            @Param("roomId") Long roomId,
            @Param("issuedAt") LocalDateTime issuedAt
    );

    @Query("""
            select queueSequence.nextQueueOrder
            from RoomQueueSequence queueSequence
            where queueSequence.roomId = :roomId
            """)
    Optional<Integer> findNextQueueOrderByRoomId(@Param("roomId") Long roomId);
}
