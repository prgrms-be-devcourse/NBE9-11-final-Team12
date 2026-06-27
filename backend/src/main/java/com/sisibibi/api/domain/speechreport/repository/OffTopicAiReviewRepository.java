package com.sisibibi.api.domain.speechreport.repository;

import com.sisibibi.api.domain.speechreport.entity.OffTopicAiReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OffTopicAiReviewRepository extends JpaRepository<OffTopicAiReview, Long> {

    Optional<OffTopicAiReview> findBySpeechId(Long speechId);

    List<OffTopicAiReview> findBySpeechIdIn(Collection<Long> speechIds);
}
