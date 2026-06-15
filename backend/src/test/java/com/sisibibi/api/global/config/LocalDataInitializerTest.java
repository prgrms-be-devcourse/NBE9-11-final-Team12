package com.sisibibi.api.global.config;

import com.sisibibi.api.domain.room.entity.Room;
import com.sisibibi.api.domain.room.repository.RoomRepository;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.topic.entity.Topic;
import com.sisibibi.api.domain.topic.repository.TopicRepository;
import com.sisibibi.api.domain.user.entity.User;
import com.sisibibi.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalDataInitializerTest {

    @Mock UserRepository userRepository;
    @Mock TopicRepository topicRepository;
    @Mock RoomRepository roomRepository;
    @Mock SpeechRepository speechRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks LocalDataInitializer initializer;

    @Test
    void doesNotCreateDuplicateSpeechWhenLocalDataAlreadyExists() {
        User testUser = org.mockito.Mockito.mock(User.class);
        User author = org.mockito.Mockito.mock(User.class);
        User admin = org.mockito.Mockito.mock(User.class);
        Topic topic = org.mockito.Mockito.mock(Topic.class);
        Room room = org.mockito.Mockito.mock(Room.class);

        when(userRepository.findByEmail("local-user@sisibibi.test")).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("local-author@sisibibi.test")).thenReturn(Optional.of(author));
        when(userRepository.findByEmail("local-admin@sisibibi.test")).thenReturn(Optional.of(admin));
        when(topicRepository.findByTitle("AI 생성 뉴스, 신뢰할 수 있을까?")).thenReturn(Optional.of(topic));
        when(topic.getId()).thenReturn(1L);
        when(roomRepository.findByTopicId(1L)).thenReturn(Optional.of(room));
        when(room.getId()).thenReturn(10L);
        when(author.getId()).thenReturn(20L);
        when(testUser.getId()).thenReturn(30L);
        when(speechRepository.existsByRoomIdAndUserIdAndDeletedFalse(10L, 20L)).thenReturn(true);

        initializer.run(new DefaultApplicationArguments(new String[0]));

        verify(speechRepository, never()).save(any());
    }
}
