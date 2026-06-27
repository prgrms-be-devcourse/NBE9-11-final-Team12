package com.sisibibi.api.domain.speechreport.repository;

import com.sisibibi.api.domain.speechreport.entity.OffTopicAiReview;
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
class OffTopicAiReviewRepositoryTest {

    @Autowired
    private OffTopicAiReviewRepository offTopicAiReviewRepository;

    @Test
    void save_assignsCreatedAtAndUpdatedAtByJpaAuditing() {
        OffTopicAiReview review = OffTopicAiReview.pending(
                10L,
                1L,
                "신고된 의견",
                5,
                5,
                50
        );

        OffTopicAiReview savedReview = offTopicAiReviewRepository.saveAndFlush(review);

        assertThat(savedReview.getCreatedAt()).isNotNull();
        assertThat(savedReview.getUpdatedAt()).isNotNull();
    }

    @Test
    void save_throwsDataIntegrityViolation_whenSpeechAlreadyHasReview() {
        offTopicAiReviewRepository.saveAndFlush(OffTopicAiReview.pending(
                10L,
                1L,
                "첫 번째 검토",
                5,
                5,
                50
        ));

        assertThatThrownBy(() -> offTopicAiReviewRepository.saveAndFlush(
                OffTopicAiReview.pending(
                        10L,
                        1L,
                        "두 번째 검토",
                        6,
                        5,
                        60
                )
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findBySpeechId_returnsReview() {
        OffTopicAiReview savedReview = offTopicAiReviewRepository.saveAndFlush(
                OffTopicAiReview.pending(
                        10L,
                        1L,
                        "신고된 의견",
                        5,
                        5,
                        50
                )
        );

        assertThat(offTopicAiReviewRepository.findBySpeechId(10L))
                .contains(savedReview);
    }
}
