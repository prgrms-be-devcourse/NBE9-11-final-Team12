package com.sisibibi.api.global.config;

import com.sisibibi.api.domain.chat.entity.ChatMessage;
import com.sisibibi.api.domain.chat.repository.ChatMessageRepository;
import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.roomparticipant.entity.RoomParticipant;
import com.sisibibi.api.domain.roomparticipant.repository.RoomParticipantRepository;
import com.sisibibi.api.domain.speech.entity.SpeakingQueueStatus;
import com.sisibibi.api.domain.speech.repository.RedisSpeakingQueueRepository;
import com.sisibibi.api.domain.speech.repository.RoomQueueSequenceRepository;
import com.sisibibi.api.domain.speech.repository.SpeakingQueueRepository;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speechreport.repository.SpeechReportRepository;
import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;

import com.sisibibi.api.global.init.LocalDataInitializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocalDataInitializerTest {

    @Mock UserRepository userRepository;
    @Mock TopicRepository topicRepository;
    @Mock RoomRepository roomRepository;
    @Mock RoomParticipantRepository roomParticipantRepository;
    @Mock SpeechRepository speechRepository;
    @Mock ChatMessageRepository chatMessageRepository;
    @Mock SpeakingQueueRepository speakingQueueRepository;
    @Mock RoomQueueSequenceRepository roomQueueSequenceRepository;
    @Mock RedisSpeakingQueueRepository redisSpeakingQueueRepository;
    @Mock SpeechReportRepository speechReportRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks
    LocalDataInitializer initializer;

    @Test
    void doesNotCreateDuplicateSpeechWhenLocalDataAlreadyExists() {
        User user = mock(User.class);
        Topic topic = mock(Topic.class);
        Room room = mock(Room.class);
        RoomParticipant participant = mock(RoomParticipant.class);
        ChatMessage chatMessage = mock(ChatMessage.class);

        when(user.getId()).thenReturn(1L);
        when(topic.getId()).thenReturn(10L);
        when(topic.getTitle()).thenReturn("local topic");
        when(room.getId()).thenReturn(100L);

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(topicRepository.findByTitle(anyString())).thenReturn(Optional.of(topic));
        when(roomRepository.findByTopicId(anyLong())).thenReturn(Optional.of(room));
        when(roomParticipantRepository.findByRoomIdAndUserId(anyLong(), anyLong()))
                .thenReturn(Optional.of(participant));
        when(speechRepository.existsByRoomIdAndUserIdAndDeletedFalse(anyLong(), anyLong()))
                .thenReturn(true);
        when(chatMessageRepository.findVisibleByRoomIdBeforeCursor(anyLong(), eq(null), any(Pageable.class)))
                .thenReturn(List.of(chatMessage));
        when(speakingQueueRepository.existsByRoomIdAndStatus(anyLong(), eq(SpeakingQueueStatus.ASSIGNED)))
                .thenReturn(true);
        when(speechRepository.findByRoomIdBeforeCursor(anyLong(), eq(null), any(Pageable.class)))
                .thenReturn(List.of());

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(speechRepository, never()).save(any());
    }
}
