package com.sisibibi.api.domain.room.repository;

import com.sisibibi.api.domain.room.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

  boolean existsByTopicId(Long topicId);
}
