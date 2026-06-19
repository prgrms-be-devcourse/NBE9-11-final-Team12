package com.sisibibi.api.domain.topic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.topic.dto.request.UpdateTopicReq;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicDetailRes;
import com.sisibibi.api.domain.topic.dto.response.topicRes.TopicSummaryRes;
import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.entity.TopicStatus;
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

@ExtendWith(MockitoExtension.class)
class TopicServiceTest {

  @Mock
  private TopicRepository topicRepository;

  @InjectMocks
  private TopicService topicService;

  @Mock
  private RoomRepository roomRepository;


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

  @Test
  void deleteTopic_deletesTopic_whenTopicHasNoRoom() {
    Topic topic = Topic.approved("제목", "설명", "IT", "https://example.com");

    when(topicRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(topic));
    when(roomRepository.existsByTopicId(1L)).thenReturn(false);

    topicService.deleteTopic(1L);

    verify(topicRepository).delete(topic);
  }

  @Test
  void deleteTopic_throwsTopicNotFound_whenTopicDoesNotExist() {
    when(topicRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> topicService.deleteTopic(999L))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.TOPIC_NOT_FOUND);
  }

  @Test
  void deleteTopic_throwsTopicHasRoom_whenTopicIsLinkedToRoom() {
    Topic topic = Topic.approved("제목", "설명", "IT", "https://example.com");

    when(topicRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(topic));
    when(roomRepository.existsByTopicId(1L)).thenReturn(true);

    assertThatThrownBy(() -> topicService.deleteTopic(1L))
        .isInstanceOf(CustomException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.TOPIC_HAS_ROOM);

    verify(topicRepository, never()).delete(topic);
  }

  @Test
  void getApprovedTopics_returnsApprovedTopicSummaries() {
    Topic firstTopic = Topic.approved("첫 번째 토픽", "설명1", "IT", "https://example.com/1");
    Topic secondTopic = Topic.approved("두 번째 토픽", "설명2", "경제", "https://example.com/2");
    Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

    when(topicRepository.findAllByStatus(TopicStatus.APPROVED, pageable))
        .thenReturn(new PageImpl<>(List.of(firstTopic, secondTopic), pageable, 2));

    Page<TopicSummaryRes> result = topicService.getApprovedTopics(pageable);

    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent().get(0).title()).isEqualTo("첫 번째 토픽");
    assertThat(result.getContent().get(0).category()).isEqualTo("IT");
    assertThat(result.getContent().get(1).title()).isEqualTo("두 번째 토픽");
    assertThat(result.getContent().get(1).category()).isEqualTo("경제");
  }
}