package com.sisibibi.api.domain.room.repository;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

  boolean existsByTopicId(Long topicId);

  List<Room> findByStatusOrderByCreatedAtDesc(RoomStatus status);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
    update Room r
    set r.status = com.sisibibi.api.domain.room.entity.RoomStatus.CLOSED
    where r.status = com.sisibibi.api.domain.room.entity.RoomStatus.OPEN
      and r.endedAt <= :now
    """)
  int closeExpiredRooms(@Param("now") LocalDateTime now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from Room room where room.id = :roomId")
    Optional<Room> findByIdForUpdate(@Param("roomId") Long roomId);
}
