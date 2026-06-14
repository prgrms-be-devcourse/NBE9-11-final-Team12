package com.sisibibi.api.domain.speech.report.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpeechReportTest {

    @Test
    void create_setsPendingStatusAndNormalizesBlankDescription() {
        SpeechReport report = SpeechReport.create(
                10L,
                30L,
                20L,
                "신고 당시 의견",
                SpeechReportReason.ABUSE_HARASSMENT,
                " "
        );

        assertThat(report.getSpeechId()).isEqualTo(10L);
        assertThat(report.getReportedUserId()).isEqualTo(30L);
        assertThat(report.getReporterUserId()).isEqualTo(20L);
        assertThat(report.getContentSnapshot()).isEqualTo("신고 당시 의견");
        assertThat(report.getReason()).isEqualTo(SpeechReportReason.ABUSE_HARASSMENT);
        assertThat(report.getDescription()).isNull();
        assertThat(report.getStatus()).isEqualTo(SpeechReportStatus.PENDING);
    }

    @Test
    void reportReasons_includeFrontendModerationCategories() {
        assertThat(SpeechReportReason.values()).contains(
                SpeechReportReason.MISINFORMATION,
                SpeechReportReason.PRIVACY_VIOLATION,
                SpeechReportReason.OFF_TOPIC
        );
    }
}
