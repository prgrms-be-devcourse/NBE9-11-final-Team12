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
            select reaction.speechId as speechId,
                   count(reaction.id) as reactionCount
            from SpeechReaction reaction, Speech speech
            where reaction.speechId = speech.id
              and speech.roomId = :roomId
              and speech.deleted = false
            group by reaction.speechId, speech.createdAt
            order by count(reaction.id) desc, speech.createdAt asc, reaction.speechId asc
            """)
    List<BestSpeechReactionProjection> findBestSpeechReactions(
            @Param("roomId") Long roomId,
            Pageable pageable
    );
}
