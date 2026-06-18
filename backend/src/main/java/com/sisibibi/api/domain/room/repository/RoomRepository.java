package com.sisibibi.api.domain.room.repository;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

  boolean existsByTopicId(Long topicId);

  List<Room> findByStatusOrderByCreatedAtDesc(RoomStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select room from Room room where room.id = :roomId")
  Optional<Room> findByIdForUpdate(@Param("roomId") Long roomId);

  List<Room> findAllByOrderByCreatedAtDesc();

  Optional<Room> findByTopicId(Long topicId);

  List<Room> findByStatusAndEndedAtLessThanEqual(
      RoomStatus status,
      LocalDateTime now,
      Pageable pageable
  );

  @Query("""
    select room.id
    from Room room
    where room.status = :status
      and room.endedAt <= :now
    order by room.endedAt asc
    """)
  List<Long> findExpiredOpenRoomIds(
      @Param("status") RoomStatus status,
      @Param("now") LocalDateTime now,
      Pageable pageable
  );

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
    update Room room
    set room.status = :closedStatus,
        room.endedAt = :closedAt
    where room.id = :roomId
      and room.status = :openStatus
      and room.endedAt <= :now
    """)
  int closeExpiredRoomIfOpen(
      @Param("roomId") Long roomId,
      @Param("openStatus") RoomStatus openStatus,
      @Param("closedStatus") RoomStatus closedStatus,
      @Param("now") LocalDateTime now,
      @Param("closedAt") LocalDateTime closedAt
  );

}
