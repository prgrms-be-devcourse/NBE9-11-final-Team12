package com.sisibibi.api.domain.report.prompt;

import com.sisibibi.api.global.exception.CustomException;
import com.sisibibi.api.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class PromptGuardHealthIndicatorTest {

    private final PromptGuardService promptGuardService = mock(PromptGuardService.class);
    private final PromptGuardHealthIndicator healthIndicator = new PromptGuardHealthIndicator(promptGuardService);

    @Test
    void healthIsUpWhenPromptGuardScanSucceeds() {
        given(promptGuardService.scan("prompt guard health check"))
                .willReturn(PromptGuardResult.allowed(PromptSeverity.SAFE, null));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("scan", "available");
    }

    @Test
    void healthIsDownWhenPromptGuardScanFails() {
        given(promptGuardService.scan("prompt guard health check"))
                .willThrow(new CustomException(ErrorCode.PROMPT_GUARD_UNAVAILABLE));

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("scan", "unavailable");
    }
}
