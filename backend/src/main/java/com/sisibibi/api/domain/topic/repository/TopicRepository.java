package com.sisibibi.api.domain.topic.repository;

import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.entity.TopicStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
  Optional<Topic> findByIdAndStatus(Long id, TopicStatus status);

  Optional<Topic> findByTitle(String title);

  Page<Topic> findAllByStatus(TopicStatus status, Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select topic from Topic topic where topic.id = :topicId")
  Optional<Topic> findByIdForUpdate(@Param("topicId") Long topicId);
}
