package com.sisibibi.api.domain.report.service;

import com.sisibibi.api.domain.report.notification.AiReportNotificationProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AiReportPdfPropertiesTest {

    @Test
    void defaultsAreConservative() {
        AiReportPdfProperties pdf = new AiReportPdfProperties();
        AiReportNotificationProperties notification = new AiReportNotificationProperties();

        assertThat(pdf.isEnabled()).isTrue();
        assertThat(pdf.getPresignedUrlExpiration()).isEqualTo(Duration.ofMinutes(10));
        assertThat(pdf.getMaxPdfRetryCount()).isEqualTo(3);
        assertThat(pdf.getMaxNotificationRetryCount()).isEqualTo(5);
        assertThat(pdf.getRetryFixedDelayMs()).isEqualTo(60000L);
        assertThat(pdf.getRetryStaleThreshold()).isEqualTo(Duration.ofMinutes(1));
        assertThat(pdf.getRetryBatchSize()).isEqualTo(20);
        assertThat(notification.getProvider()).isEqualTo("smtp");
        assertThat(notification.getFromEmail()).isEmpty();
        assertThat(notification.getHomepageUrl()).isEqualTo("http://localhost:3000");
    }
}
