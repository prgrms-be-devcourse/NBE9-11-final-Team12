package com.sisibibi.api.domain.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.entity.RoomQueueSequence;
import com.sisibibi.api.domain.speech.repository.RoomQueueSequenceRepository;
import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoomCreateCommandServiceTest {

  @Mock
  private RoomRepository roomRepository;

  @Mock
  private TopicRepository topicRepository;

  @Mock
  private RoomQueueSequenceRepository roomQueueSequenceRepository;

  @InjectMocks
  private RoomCreateCommandService roomCreateCommandService;

  @Test
  void createRoom_setsDefaultDurationToTwoHours() {
    Topic topic = Topic.approved(
        "approved topic",
        "description",
        "category",
        "https://example.com"
    );
    ReflectionTestUtils.setField(topic, "id", 1L);

    given(topicRepository.findByIdForUpdate(1L)).willReturn(Optional.of(topic));
    given(roomRepository.existsByTopicId(1L)).willReturn(false);
    given(roomRepository.save(any(Room.class))).willAnswer(invocation -> invocation.getArgument(0));

    roomCreateCommandService.createRoom(1L, "debate title", 100);

    ArgumentCaptor<Room> roomCaptor = ArgumentCaptor.forClass(Room.class);
    verify(roomRepository).save(roomCaptor.capture());
    verify(roomQueueSequenceRepository).save(any(RoomQueueSequence.class));

    Room savedRoom = roomCaptor.getValue();
    assertThat(Duration.between(savedRoom.getStartedAt(), savedRoom.getEndedAt()))
        .isEqualTo(Duration.ofHours(2));
  }
}
