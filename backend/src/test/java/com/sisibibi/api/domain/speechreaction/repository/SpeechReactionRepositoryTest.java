package com.sisibibi.api.domain.speechreaction.repository;

import com.sisibibi.api.domain.speech.entity.Speech;
import com.sisibibi.api.domain.speech.entity.SpeechStance;
import com.sisibibi.api.domain.speech.repository.SpeechRepository;
import com.sisibibi.api.domain.speechreaction.entity.SpeechReaction;
import com.sisibibi.api.domain.speechreaction.repository.projection.BestSpeechReactionProjection;
import com.sisibibi.api.domain.speechreaction.repository.projection.SpeechReactionSummaryProjection;
import com.sisibibi.api.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class SpeechReactionRepositoryTest {

    @Autowired
    private SpeechReactionRepository speechReactionRepository;

    @Autowired
    private SpeechRepository speechRepository;

    @Test
    void save_assignsCreatedAtByJpaAuditing() {
        SpeechReaction saved = speechReactionRepository.saveAndFlush(
                SpeechReaction.create(10L, 20L)
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void save_throwsDataIntegrityViolation_whenReactionIsDuplicated() {
        speechReactionRepository.saveAndFlush(SpeechReaction.create(10L, 20L));

        assertThatThrownBy(() -> speechReactionRepository.saveAndFlush(
                SpeechReaction.create(10L, 20L)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findBySpeechIdAndUserId_returnsReaction() {
        speechReactionRepository.saveAndFlush(SpeechReaction.create(10L, 20L));

        assertThat(speechReactionRepository.findBySpeechIdAndUserId(10L, 20L))
                .isPresent();
    }

    @Test
    void findReactionSummaries_returnsCountsAndCurrentUserReactionInSingleQuery() {
        Speech first = speechRepository.saveAndFlush(
                Speech.createMainOpinion(1L, 10L, "첫 번째 의견", SpeechStance.PRO)
        );
        Speech second = speechRepository.saveAndFlush(
                Speech.createMainOpinion(1L, 20L, "두 번째 의견", SpeechStance.CON)
        );
        Long currentUserId = 100L;

        speechReactionRepository.saveAllAndFlush(java.util.List.of(
                SpeechReaction.create(first.getId(), currentUserId),
                SpeechReaction.create(first.getId(), 101L),
                SpeechReaction.create(second.getId(), 102L)
        ));

        java.util.List<SpeechReactionSummaryProjection> results =
                speechReactionRepository.findReactionSummaries(
                        java.util.List.of(first.getId(), second.getId()),
                        currentUserId
                );

        assertThat(results)
                .extracting(
                        SpeechReactionSummaryProjection::getSpeechId,
                        SpeechReactionSummaryProjection::getReactionCount,
                        SpeechReactionSummaryProjection::getMyReactionCount
                )
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(first.getId(), 2L, 1L),
                        org.assertj.core.groups.Tuple.tuple(second.getId(), 1L, 0L)
                );
    }

    @Test
    void findBestSpeechReactions_ordersByReactionCountThenCreatedOrder() {
        Speech mostReacted = speechRepository.saveAndFlush(
                Speech.createMainOpinion(1L, 10L, "공감 2개", SpeechStance.PRO)
        );
        Speech tiedOlder = speechRepository.saveAndFlush(
                Speech.createMainOpinion(1L, 20L, "공감 1개 이전", SpeechStance.CON)
        );
        Speech tiedLatest = speechRepository.saveAndFlush(
                Speech.createMainOpinion(1L, 30L, "공감 1개 최신", SpeechStance.PRO)
        );
        Speech deleted = speechRepository.saveAndFlush(
                Speech.createMainOpinion(1L, 40L, "삭제된 공감 3개", SpeechStance.CON)
        );
        deleted.softDelete(java.time.LocalDateTime.of(2026, 6, 19, 12, 0));

        speechReactionRepository.saveAllAndFlush(java.util.List.of(
                SpeechReaction.create(mostReacted.getId(), 101L),
                SpeechReaction.create(mostReacted.getId(), 102L),
                SpeechReaction.create(tiedOlder.getId(), 103L),
                SpeechReaction.create(tiedLatest.getId(), 104L),
                SpeechReaction.create(deleted.getId(), 105L),
                SpeechReaction.create(deleted.getId(), 106L),
                SpeechReaction.create(deleted.getId(), 107L)
        ));

        java.util.List<BestSpeechReactionProjection> results =
                speechReactionRepository.findBestSpeechReactions(
                        1L,
                        PageRequest.of(0, 3)
                );

        assertThat(results).extracting(BestSpeechReactionProjection::getSpeechId)
                .containsExactly(
                        mostReacted.getId(),
                        tiedOlder.getId(),
                        tiedLatest.getId()
                );
        assertThat(results).extracting(BestSpeechReactionProjection::getReactionCount)
                .containsExactly(2L, 1L, 1L);
    }
}
