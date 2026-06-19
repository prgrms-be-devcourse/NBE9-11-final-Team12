package com.sisibibi.api.domain.speechreaction.repository;

import com.sisibibi.api.domain.speechreaction.entity.SpeechReaction;
import com.sisibibi.api.domain.speechreaction.repository.projection.BestSpeechReactionProjection;
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
                   count(reaction.id) as reactionCount
            from SpeechReaction reaction
            where reaction.speechId in (
                select speech.id
                from Speech speech
                where speech.roomId = :roomId
                  and speech.deleted = false
            )
            group by reaction.speechId
            order by count(reaction.id) desc, reaction.speechId desc
            """)
    List<BestSpeechReactionProjection> findBestSpeechReactions(
            @Param("roomId") Long roomId,
            Pageable pageable
    );
}
