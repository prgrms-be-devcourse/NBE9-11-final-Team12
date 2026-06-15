package com.sisibibi.api.domain.speech.repository;

import com.sisibibi.api.domain.speech.entity.Speech;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpeechRepository extends JpaRepository<Speech, Long> {

    Optional<Speech> findByIdAndDeletedFalse(Long id);

    boolean existsByRoomIdAndUserIdAndDeletedFalse(Long roomId, Long userId);

    @Query("""
            select speech
            from Speech speech
            where speech.roomId = :roomId
              and speech.deleted = false
              and (:cursor is null or speech.id < :cursor)
            order by speech.id desc
            """)
    List<Speech> findByRoomIdBeforeCursor(
            @Param("roomId") Long roomId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );
}
