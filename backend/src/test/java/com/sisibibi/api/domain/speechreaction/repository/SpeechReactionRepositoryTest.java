package com.sisibibi.api.domain.speechreaction.repository;

import com.sisibibi.api.domain.speechreaction.entity.SpeechReaction;
import com.sisibibi.api.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class SpeechReactionRepositoryTest {

    @Autowired
    private SpeechReactionRepository speechReactionRepository;

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
}
