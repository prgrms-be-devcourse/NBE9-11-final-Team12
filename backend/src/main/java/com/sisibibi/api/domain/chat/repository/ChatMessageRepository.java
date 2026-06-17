package com.sisibibi.api.domain.chat.repository;

import com.sisibibi.api.domain.chat.entity.ChatMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByIdAndRoomIdAndDeletedFalse(Long id, Long roomId);

    @Query("""
            select message
            from ChatMessage message
            where message.roomId = :roomId
              and message.deleted = false
              and (:cursor is null or message.id < :cursor)
            order by message.id desc
            """)
    List<ChatMessage> findVisibleByRoomIdBeforeCursor(
            @Param("roomId") Long roomId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
