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
}
