package com.sisibibi.api.domain.report.prompt;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("promptGuard")
@RequiredArgsConstructor
public class PromptGuardHealthIndicator implements HealthIndicator {

    private static final String HEALTH_CHECK_CONTENT = "prompt guard health check";

    private final PromptGuardService promptGuardService;

    @Override
    public Health health() {
        try {
            promptGuardService.scan(HEALTH_CHECK_CONTENT);
            return Health.up()
                    .withDetail("scan", "available")
                    .build();
        } catch (RuntimeException e) {
            return Health.down(e)
                    .withDetail("scan", "unavailable")
                    .build();
        }
    }
}
