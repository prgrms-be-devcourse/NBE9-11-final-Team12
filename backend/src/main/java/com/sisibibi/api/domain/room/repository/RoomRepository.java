package com.sisibibi.api.domain.room.repository;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.entity.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

  boolean existsByTopicId(Long topicId);

  List<Room> findByStatusOrderByCreatedAtDesc(RoomStatus status);

  List<Room> findByStatusAndEndedAtLessThanEqual(RoomStatus status, LocalDateTime now);
}
