package com.sisibibi.api.domain.report.repository;

import com.sisibibi.api.domain.report.service.AiReportPdfModel;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.entity.SpeechStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AiReportPdfQueryRepository {

    private final EntityManager entityManager;

    public int countParticipants(Long roomId) {
        Long count = entityManager.createQuery("""
                select count(participant.id)
                from RoomParticipant participant
                where participant.roomId = :roomId
                """, Long.class)
                .setParameter("roomId", roomId)
                .getSingleResult();
        return count.intValue();
    }

    public AiReportPdfModel.Stats collectStats(Long roomId) {
        Long opinionCount = entityManager.createQuery("""
                select count(speech.id)
                from Speech speech
                where speech.roomId = :roomId
                  and speech.status = :completed
                  and speech.deleted = false
                  and speech.stance is not null
                  and speech.content is not null
                  and trim(speech.content) <> ''
                """, Long.class)
                .setParameter("roomId", roomId)
                .setParameter("completed", SpeechStatus.COMPLETED)
                .getSingleResult();

        Long reactionCount = entityManager.createQuery("""
                select count(reaction.id)
                from SpeechReaction reaction, Speech speech
                where reaction.speechId = speech.id
                  and speech.roomId = :roomId
                  and speech.status = :completed
                  and speech.deleted = false
                  and speech.content is not null
                  and trim(speech.content) <> ''
                """, Long.class)
                .setParameter("roomId", roomId)
                .setParameter("completed", SpeechStatus.COMPLETED)
                .getSingleResult();

        Long proCount = countByStance(roomId, SpeechStance.PRO);
        Long conCount = countByStance(roomId, SpeechStance.CON);
        return new AiReportPdfModel.Stats(opinionCount, reactionCount, proCount, conCount);
    }

    public List<AiReportPdfModel.TopOpinion> findTopOpinions(Long roomId, SpeechStance stance, int limit) {
        return entityManager.createQuery("""
                select new com.sisibibi.api.domain.report.service.AiReportPdfModel$TopOpinion(
                    speech.id,
                    speech.userId,
                    user.nickname,
                    speech.stance,
                    speech.content,
                    count(reaction.id),
                    speech.createdAt
                )
                from Speech speech
                join User user on user.id = speech.userId
                left join SpeechReaction reaction on reaction.speechId = speech.id
                where speech.roomId = :roomId
                  and speech.status = :completed
                  and speech.deleted = false
                  and speech.stance = :stance
                  and speech.content is not null
                  and trim(speech.content) <> ''
                group by speech.id, speech.userId, user.nickname, speech.stance, speech.content, speech.createdAt
                order by count(reaction.id) desc, speech.createdAt asc, speech.id asc
                """, AiReportPdfModel.TopOpinion.class)
                .setParameter("roomId", roomId)
                .setParameter("completed", SpeechStatus.COMPLETED)
                .setParameter("stance", stance)
                .setMaxResults(limit)
                .getResultList();
    }

    private Long countByStance(Long roomId, SpeechStance stance) {
        return entityManager.createQuery("""
                select count(speech.id)
                from Speech speech
                where speech.roomId = :roomId
                  and speech.status = :completed
                  and speech.deleted = false
                  and speech.stance = :stance
                  and speech.content is not null
                  and trim(speech.content) <> ''
                """, Long.class)
                .setParameter("roomId", roomId)
                .setParameter("completed", SpeechStatus.COMPLETED)
                .setParameter("stance", stance)
                .getSingleResult();
    }
}
