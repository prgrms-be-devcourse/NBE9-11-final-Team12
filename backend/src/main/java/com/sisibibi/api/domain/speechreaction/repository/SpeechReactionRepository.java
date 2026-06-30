package com.sisibibi.api.domain.speechreaction.repository;

import com.sisibibi.api.domain.speechreaction.entity.SpeechReaction;
import com.sisibibi.api.domain.speechreaction.repository.projection.BestSpeechReactionProjection;
import com.sisibibi.api.domain.speechreaction.repository.projection.SpeechReactionSummaryProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpeechReactionRepository extends JpaRepository<SpeechReaction, Long> {

    boolean existsBySpeechIdAndUserId(Long speechId, Long userId);

    Optional<SpeechReaction> findBySpeechIdAndUserId(Long speechId, Long userId);

    long countBySpeechId(Long speechId);

    @Query("""
            select count(reaction.id)
            from SpeechReaction reaction, Speech speech
            where reaction.speechId = speech.id
              and speech.userId = :userId
              and speech.deleted = false
            """)
    long countReceivedByUserId(@Param("userId") Long userId);

    @Query("""
            select reaction.speechId as speechId,
                   count(reaction.id) as reactionCount,
                   sum(case when reaction.userId = :userId then 1 else 0 end) as myReactionCount
            from SpeechReaction reaction
            where reaction.speechId in :speechIds
            group by reaction.speechId
            """)
    List<SpeechReactionSummaryProjection> findReactionSummaries(
            @Param("speechIds") List<Long> speechIds,
            @Param("userId") Long userId
    );

    @Query("""
            select speech.id as speechId,
                   speech.roomId as roomId,
                   speech.userId as userId,
                   speech.content as content,
                   speech.stance as stance,
                   speech.status as status,
                   speech.createdAt as createdAt,
                   count(reaction.id) as reactionCount
            from SpeechReaction reaction, Speech speech
            where reaction.speechId = speech.id
              and speech.roomId = :roomId
              and speech.deleted = false
            group by speech.id,
                     speech.roomId,
                     speech.userId,
                     speech.content,
                     speech.stance,
                     speech.status,
                     speech.createdAt
            order by count(reaction.id) desc, speech.createdAt asc, speech.id asc
            """)
    List<BestSpeechReactionProjection> findBestSpeechReactions(
            @Param("roomId") Long roomId,
            Pageable pageable
    );

    @Query("""
            select count(reaction.id)
            from SpeechReaction reaction, Speech speech
            where reaction.speechId = speech.id
              and speech.roomId = :roomId
              and speech.status = com.sisibibi.api.domain.speech.entity.SpeechStatus.COMPLETED
              and speech.deleted = false
              and speech.content is not null
              and trim(speech.content) <> ''
            """)
    long countReactionsForCompletedSpeeches(@Param("roomId") Long roomId);

    @Query("""
        select count(reaction.id)
        from SpeechReaction reaction, Speech speech
        where reaction.speechId = speech.id
          and speech.roomId = :roomId
          and speech.deleted = false
        """)
    long countByRoomId(@Param("roomId") Long roomId);
}
