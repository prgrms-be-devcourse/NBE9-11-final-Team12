package com.sisibibi.api.domain.topic.repository;

import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.entity.TopicStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
  Optional<Topic> findByIdAndStatus(Long id, TopicStatus status);

  Optional<Topic> findByTitle(String title);

  Page<Topic> findAllByStatus(TopicStatus status, Pageable pageable);
}
