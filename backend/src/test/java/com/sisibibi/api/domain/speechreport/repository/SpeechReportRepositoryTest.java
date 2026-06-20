package com.sisibibi.api.domain.speechreport.repository;

import com.sisibibi.api.domain.speechreport.entity.SpeechReport;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportReason;
import com.sisibibi.api.domain.speechreport.entity.SpeechReportStatus;
import com.sisibibi.api.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class SpeechReportRepositoryTest {

    @Autowired
    private SpeechReportRepository speechReportRepository;

    @Test
    void save_assignsCreatedAtAndUpdatedAtByJpaAuditing() {
        SpeechReport report = SpeechReport.create(
                10L,
                30L,
                20L,
                "신고 대상 의견",
                SpeechReportReason.SPAM,
                null
        );

        assertThat(report.getCreatedAt()).isNull();
        assertThat(report.getUpdatedAt()).isNull();

        SpeechReport savedReport = speechReportRepository.saveAndFlush(report);

        assertThat(savedReport.getCreatedAt()).isNotNull();
        assertThat(savedReport.getUpdatedAt()).isNotNull();
    }

    @Test
    void existsBySpeechIdAndReporterUserId_returnsTrue_whenReportExists() {
        speechReportRepository.saveAndFlush(SpeechReport.create(
                10L,
                30L,
                20L,
                "신고 대상 의견",
                SpeechReportReason.SPAM,
                null
        ));

        assertThat(speechReportRepository.existsBySpeechIdAndReporterUserId(10L, 20L))
                .isTrue();
    }

    @Test
    void save_throwsDataIntegrityViolation_whenReportIsDuplicated() {
        speechReportRepository.saveAndFlush(SpeechReport.create(
                10L,
                30L,
                20L,
                "신고 대상 의견",
                SpeechReportReason.SPAM,
                null
        ));

        assertThatThrownBy(() -> speechReportRepository.saveAndFlush(SpeechReport.create(
                10L,
                30L,
                20L,
                "수정된 신고 대상 의견",
                SpeechReportReason.HATE_SPEECH,
                "다른 사유"
        )))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findAllByFilters_filtersByStatusAndReason() {
        speechReportRepository.saveAndFlush(SpeechReport.create(
                10L,
                30L,
                20L,
                "스팸 신고 대상",
                SpeechReportReason.SPAM,
                null
        ));
        speechReportRepository.saveAndFlush(SpeechReport.create(
                11L,
                31L,
                21L,
                "혐오 발언 신고 대상",
                SpeechReportReason.HATE_SPEECH,
                null
        ));

        Page<SpeechReport> reports = speechReportRepository.findAllByFilters(
                SpeechReportStatus.PENDING,
                SpeechReportReason.SPAM,
                PageRequest.of(0, 20)
        );

        assertThat(reports.getTotalElements()).isEqualTo(1);
        assertThat(reports.getContent().getFirst().getReason())
                .isEqualTo(SpeechReportReason.SPAM);
    }
}
