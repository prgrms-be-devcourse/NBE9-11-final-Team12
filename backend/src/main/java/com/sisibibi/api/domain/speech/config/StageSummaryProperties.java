package com.sisibibi.api.domain.speech.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.stage-summary")
public class StageSummaryProperties {

    private boolean enabled = true;
    private Duration generateTimeout = Duration.ofSeconds(10);
    private int maxGenerationAttempts = 3;
    private int minCompletedSpeakerCount = 10;
    private int recoveryBatchSize = 20;
}
