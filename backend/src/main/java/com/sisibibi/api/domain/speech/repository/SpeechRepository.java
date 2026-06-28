package com.sisibibi.api.domain.speech.repository;

import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpeechRepository extends JpaRepository<Speech, Long> {

    Optional<Speech> findByIdAndDeletedFalse(Long id);

    boolean existsByRoomIdAndUserIdAndDeletedFalse(Long roomId, Long userId);

    long countByUserIdAndDeletedFalse(Long userId);

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

    @Query("""
            select speech
            from Speech speech
            where speech.roomId = :roomId
              and (:cursor is null or speech.id < :cursor)
            order by speech.id desc
            """)
    List<Speech> findByRoomIdBeforeCursorIncludingDeleted(
            @Param("roomId") Long roomId,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    @Query("""
            select speech
            from Speech speech
            where speech.roomId = :roomId
              and speech.deleted = false
              and speech.status = com.sisibibi.api.domain.speech.entity.SpeechStatus.COMPLETED
              and speech.stance is not null
              and speech.content is not null
              and trim(speech.content) <> ''
            order by speech.createdAt asc, speech.id asc
            """)
    List<Speech> findAiReportSourceSpeeches(@Param("roomId") Long roomId);

    @Query("""
            select count(speech.id)
            from Speech speech
            where speech.roomId = :roomId
              and speech.deleted = false
              and speech.status = com.sisibibi.api.domain.speech.entity.SpeechStatus.COMPLETED
              and speech.stance is not null
              and speech.content is not null
              and trim(speech.content) <> ''
            """)
    long countAiReportSourceSpeeches(@Param("roomId") Long roomId);

    @Query("""
            select count(speech.id)
            from Speech speech
            where speech.roomId = :roomId
              and speech.deleted = false
              and speech.status = com.sisibibi.api.domain.speech.entity.SpeechStatus.COMPLETED
              and speech.stance = :stance
              and speech.content is not null
              and trim(speech.content) <> ''
            """)
    long countAiReportSourceSpeechesByStance(@Param("roomId") Long roomId, @Param("stance") SpeechStance stance);

    @Query("""
            select speech
            from Speech speech
            where speech.roomId = :roomId
              and speech.deleted = false
              and speech.content is not null
              and function('regexp_replace', speech.content, '\\s+', '') <> ''
              and speech.createdAt <= :triggeredAt
            order by speech.createdAt asc, speech.id asc
            """)
    List<Speech> findStageSummarySourceSpeeches(
            @Param("roomId") Long roomId,
            @Param("triggeredAt") LocalDateTime triggeredAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Speech speech
            set speech.status = :completedStatus,
                speech.endedAt = :endedAt
            where speech.roomId = :roomId
              and speech.userId = :userId
              and speech.deleted = false
              and speech.status = :speakingStatus
            """)
    int completeSpeakingSpeeches(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId,
            @Param("speakingStatus") SpeechStatus speakingStatus,
            @Param("completedStatus") SpeechStatus completedStatus,
            @Param("endedAt") LocalDateTime endedAt
    );
}
