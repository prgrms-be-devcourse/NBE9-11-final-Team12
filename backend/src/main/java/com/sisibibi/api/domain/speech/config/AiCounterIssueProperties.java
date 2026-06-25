package com.sisibibi.api.domain.speech.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai-counter-issue")
public class AiCounterIssueProperties {

    private Duration generateTimeout = Duration.ofSeconds(10);
    private int maxGenerationAttempts = 3;
}
