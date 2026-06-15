package com.sisibibi.api.domain.speech.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpUrlValidatorTest {

    private final HttpUrlValidator validator = new HttpUrlValidator();

    @Test
    void isValid_returnsTrue_forHttpAndHttpsUrlsWithHost() {
        assertThat(validator.isValid("http://example.com/evidence", null)).isTrue();
        assertThat(validator.isValid("https://example.com/evidence", null)).isTrue();
    }

    @Test
    void isValid_returnsFalse_forRelativePathOrMissingHost() {
        assertThat(validator.isValid("/evidence", null)).isFalse();
        assertThat(validator.isValid("https:///evidence", null)).isFalse();
    }

    @Test
    void isValid_returnsFalse_forUnsupportedSchemeOrInvalidSyntax() {
        assertThat(validator.isValid("ftp://example.com/evidence", null)).isFalse();
        assertThat(validator.isValid("https://example .com/evidence", null)).isFalse();
    }

    @Test
    void isValid_leavesBlankValidationToNotBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid(" ", null)).isTrue();
    }
}
