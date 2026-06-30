package com.sisibibi.api.domain.report.prompt;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PromptGuardPropertiesTest {

    @Test
    void defaultTimeoutsAreShortForSynchronousReportRequests() {
        PromptGuardProperties properties = new PromptGuardProperties();

        assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofMillis(500));
        assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofMillis(1000));
    }

    @Test
    void defaultCustomPromptMaxCountMatchesProductPolicy() {
        PromptGuardProperties properties = new PromptGuardProperties();

        assertThat(properties.getCustomPromptMaxCount()).isEqualTo(3);
    }
}
