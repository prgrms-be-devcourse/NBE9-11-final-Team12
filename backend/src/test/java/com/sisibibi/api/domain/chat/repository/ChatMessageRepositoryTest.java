package com.sisibibi.api.domain.chat.repository;

import com.sisibibi.api.domain.chat.entity.ChatMessage;
import com.sisibibi.api.global.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Test
    void findVisibleByRoomIdBeforeCursor_returnsLatestMessagesByMessageId() {
        ChatMessage first = chatMessageRepository.save(ChatMessage.create(1L, 2L, "a", "first"));
        ChatMessage second = chatMessageRepository.save(ChatMessage.create(1L, 3L, "b", "second"));
        ChatMessage third = chatMessageRepository.save(ChatMessage.create(1L, 4L, "c", "third"));
        ChatMessage otherRoom = chatMessageRepository.save(ChatMessage.create(2L, 2L, "a", "other"));
        otherRoom.softDelete(2L, LocalDateTime.now());
        chatMessageRepository.flush();

        List<ChatMessage> result = chatMessageRepository.findVisibleByRoomIdBeforeCursor(
                1L,
                null,
                PageRequest.of(0, 2)
        );

        assertThat(result).extracting(ChatMessage::getId)
                .containsExactly(third.getId(), second.getId());
        assertThat(result).extracting(ChatMessage::getContent)
                .doesNotContain(otherRoom.getContent());
        assertThat(first.getId()).isLessThan(second.getId());
    }

    @Test
    void findVisibleByRoomIdBeforeCursor_usesMessageIdCursorAndExcludesDeletedMessages() {
        ChatMessage first = chatMessageRepository.save(ChatMessage.create(1L, 2L, "a", "first"));
        ChatMessage second = chatMessageRepository.save(ChatMessage.create(1L, 3L, "b", "second"));
        ChatMessage third = chatMessageRepository.save(ChatMessage.create(1L, 4L, "c", "third"));
        second.softDelete(3L, LocalDateTime.now());
        chatMessageRepository.flush();

        List<ChatMessage> result = chatMessageRepository.findVisibleByRoomIdBeforeCursor(
                1L,
                third.getId(),
                PageRequest.of(0, 10)
        );

        assertThat(result).extracting(ChatMessage::getId).containsExactly(first.getId());
    }

    @Test
    void findByIdAndRoomIdAndDeletedFalse_excludesDeletedMessage() {
        ChatMessage message = chatMessageRepository.save(ChatMessage.create(1L, 2L, "a", "hello"));
        message.softDelete(2L, LocalDateTime.now());
        chatMessageRepository.flush();

        assertThat(chatMessageRepository.findByIdAndRoomIdAndDeletedFalse(message.getId(), 1L))
                .isEmpty();
    }
}
