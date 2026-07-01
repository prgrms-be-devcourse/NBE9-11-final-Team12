package com.sisibibi.api.domain.report.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptSeverityTest {

    @Test
    void from_returnsUnknown_whenValueIsNullOrBlank() {
        assertThat(PromptSeverity.from(null)).isEqualTo(PromptSeverity.UNKNOWN);
        assertThat(PromptSeverity.from(" ")).isEqualTo(PromptSeverity.UNKNOWN);
    }

    @Test
    void from_mapsNumericValues() {
        assertThat(PromptSeverity.from("0")).isEqualTo(PromptSeverity.SAFE);
        assertThat(PromptSeverity.from("1")).isEqualTo(PromptSeverity.LOW);
        assertThat(PromptSeverity.from("2")).isEqualTo(PromptSeverity.MEDIUM);
        assertThat(PromptSeverity.from("3")).isEqualTo(PromptSeverity.HIGH);
        assertThat(PromptSeverity.from("4")).isEqualTo(PromptSeverity.CRITICAL);
    }

    @Test
    void from_mapsEnumNameIgnoringCaseAndWhitespace() {
        assertThat(PromptSeverity.from(" safe ")).isEqualTo(PromptSeverity.SAFE);
        assertThat(PromptSeverity.from("low")).isEqualTo(PromptSeverity.LOW);
        assertThat(PromptSeverity.from("Medium")).isEqualTo(PromptSeverity.MEDIUM);
        assertThat(PromptSeverity.from("HIGH")).isEqualTo(PromptSeverity.HIGH);
        assertThat(PromptSeverity.from("critical")).isEqualTo(PromptSeverity.CRITICAL);
    }

    @Test
    void from_returnsUnknown_whenValueIsUnsupported() {
        assertThat(PromptSeverity.from("5")).isEqualTo(PromptSeverity.UNKNOWN);
        assertThat(PromptSeverity.from("danger")).isEqualTo(PromptSeverity.UNKNOWN);
    }

    @Test
    void blocksRequest_returnsTrueOnlyForMediumOrHigherKnownRisk() {
        assertThat(PromptSeverity.SAFE.blocksRequest()).isFalse();
        assertThat(PromptSeverity.LOW.blocksRequest()).isFalse();
        assertThat(PromptSeverity.MEDIUM.blocksRequest()).isTrue();
        assertThat(PromptSeverity.HIGH.blocksRequest()).isTrue();
        assertThat(PromptSeverity.CRITICAL.blocksRequest()).isTrue();
        assertThat(PromptSeverity.UNKNOWN.blocksRequest()).isFalse();
    }

    @Test
    void rank_ordersKnownSeverityAndPlacesUnknownBelowSafe() {
        assertThat(PromptSeverity.UNKNOWN.rank()).isEqualTo(-1);
        assertThat(PromptSeverity.SAFE.rank()).isZero();
        assertThat(PromptSeverity.LOW.rank()).isEqualTo(1);
        assertThat(PromptSeverity.MEDIUM.rank()).isEqualTo(2);
        assertThat(PromptSeverity.HIGH.rank()).isEqualTo(3);
        assertThat(PromptSeverity.CRITICAL.rank()).isEqualTo(4);
    }
}
