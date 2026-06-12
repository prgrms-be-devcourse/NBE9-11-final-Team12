package com.sisibibi.api.domain.topic.service;

import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.topic.dto.request.CreateTopicReq;
import com.sisibibi.api.domain.topic.dto.request.UpdateTopicReq;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicCreateRes;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicDetailRes;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicSummaryRes;
import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.entity.TopicStatus;
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopicService {
  private final TopicRepository topicRepository;
  private final RoomRepository roomRepository;

  public TopicDetailRes getTopicDetail(Long topicId) {
    Topic topic = topicRepository.findByIdAndStatus(topicId, TopicStatus.APPROVED)
        .orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));

    return TopicDetailRes.from(topic);
  }

  @Transactional
  public TopicCreateRes createApprovedTopic(CreateTopicReq request) {
    Topic topic = Topic.approved(
        request.title().trim(),
        request.description(),
        request.category().trim(),
        request.sourceUrl()
    );

    Topic savedTopic = topicRepository.save(topic);

    return TopicCreateRes.from(savedTopic);
  }

  @Transactional
  public TopicDetailRes updateTopic(Long topicId, UpdateTopicReq request) {
    Topic topic = topicRepository.findById(topicId)
        .orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));

    topic.update(
        request.title().trim(),
        request.description(),
        request.category().trim(),
        request.sourceUrl()
    );

    return TopicDetailRes.from(topic);
  }

  @Transactional
  public void deleteTopic(Long topicId) {
    Topic topic = topicRepository.findById(topicId)
        .orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));

    if (roomRepository.existsByTopicId(topicId)) {
      throw new CustomException(ErrorCode.TOPIC_HAS_ROOM);
    }

    topicRepository.delete(topic);
  }

  public List<TopicSummaryRes> getApprovedTopics() {
    return topicRepository.findAllByStatusOrderByCreatedAtDesc(TopicStatus.APPROVED)
        .stream()
        .map(TopicSummaryRes::from)
        .toList();
  }


}
