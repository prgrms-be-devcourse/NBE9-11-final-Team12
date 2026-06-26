package com.sisibibi.api.domain.speech.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sisibibi.api.global.config.JpaAuditingConfig;
import com.sisibibi.api.domain.speech.entity.StageSummary;
import com.sisibibi.api.domain.speech.entity.StageSummaryStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class StageSummaryRepositoryTest {

    @Autowired
    private StageSummaryRepository stageSummaryRepository;

    @Test
    void save_rejectsDuplicateSummaryForSameRoom() {
        stageSummaryRepository.saveAndFlush(StageSummary.pending(
                1L,
                LocalDateTime.of(2026, 6, 26, 12, 30),
                10
        ));

        assertThatThrownBy(() -> stageSummaryRepository.saveAndFlush(StageSummary.pending(
                1L,
                LocalDateTime.of(2026, 6, 26, 12, 31),
                11
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findRetryCandidates_returnsFailedSummariesBelowMaxAttemptsInCreatedOrder() {
        StageSummary retryFirst = StageSummary.pending(
                1L,
                LocalDateTime.of(2026, 6, 26, 12, 30),
                10
        );
        retryFirst.fail("temporary failure");
        StageSummary exhausted = StageSummary.pending(
                2L,
                LocalDateTime.of(2026, 6, 26, 12, 31),
                10
        );
        exhausted.fail("failure one");
        exhausted.markPendingForRetry();
        exhausted.fail("failure two");
        exhausted.markPendingForRetry();
        exhausted.fail("failure three");
        StageSummary completed = StageSummary.pending(
                3L,
                LocalDateTime.of(2026, 6, 26, 12, 32),
                10
        );
        completed.complete(
                "중간 정리",
                List.of("쟁점 1", "쟁점 2", "쟁점 3"),
                12,
                LocalDateTime.of(2026, 6, 26, 12, 33)
        );
        StageSummary retrySecond = StageSummary.pending(
                4L,
                LocalDateTime.of(2026, 6, 26, 12, 34),
                12
        );
        retrySecond.fail("temporary failure");
        stageSummaryRepository.saveAllAndFlush(List.of(
                retryFirst,
                exhausted,
                completed,
                retrySecond
        ));

        List<StageSummary> candidates = stageSummaryRepository.findRetryCandidates(
                StageSummaryStatus.FAILED,
                3,
                PageRequest.of(0, 10)
        );

        assertThat(candidates)
                .extracting(StageSummary::getRoomId)
                .containsExactly(1L, 4L);
    }
}
