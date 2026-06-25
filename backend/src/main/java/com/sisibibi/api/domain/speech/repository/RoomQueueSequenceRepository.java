package com.sisibibi.api.domain.speech.repository;

import com.sisibibi.api.domain.speech.entity.RoomQueueSequence;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
}
