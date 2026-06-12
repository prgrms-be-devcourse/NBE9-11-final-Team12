package com.sisibibi.api.domain.topic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sisibibi.api.domain.topic.dto.request.UpdateTopicReq;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicDetailRes;
import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

  @Mock
  private TopicRepository topicRepository;

  @InjectMocks
  private TopicService topicService;

  @Test
  void updateTopic_updatesTopicFields() {
    Topic topic = Topic.approved("기존 제목", "기존 설명", "IT", "https://old.example.com");
    UpdateTopicReq request = new UpdateTopicReq(
        "수정 제목",
        "수정 설명",
        "경제",
        "https://new.example.com"
    );

    when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));

    TopicDetailRes result = topicService.updateTopic(1L, request);

    assertThat(result.title()).isEqualTo("수정 제목");
    assertThat(result.description()).isEqualTo("수정 설명");
    assertThat(result.category()).isEqualTo("경제");
    assertThat(result.sourceUrl()).isEqualTo("https://new.example.com");
  }

  @Test
  void updateTopic_throwsTopicNotFound_whenTopicDoesNotExist() {
    UpdateTopicReq request = new UpdateTopicReq("수정 제목", "수정 설명", "IT", "https://example.com");

    when(topicRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> topicService.updateTopic(999L, request))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.TOPIC_NOT_FOUND);
  }
}