package com.sisibibi.api.domain.topic.service;

import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicDetailRes;
import com.sisibibi.api.domain.topic.entity.TopicStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicService {
  private final TopicRepository topicRepository;

  public TopicDetailRes getTopicDetail(Long topicId) {
    Topic topic = topicRepository.findByIdAndStatus(topicId, TopicStatus.APPROVED)
        .orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));

    return TopicDetailRes.from(topic);
  }
}
