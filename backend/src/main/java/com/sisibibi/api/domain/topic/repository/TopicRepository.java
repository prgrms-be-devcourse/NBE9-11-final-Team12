package com.sisibibi.api.domain.topic.repository;

import com.sisibibi.api.domain.topic.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Long> {
}
