package com.sisibibi.api.domain.speechreport.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.off-topic-ai-review")
public class OffTopicAiReviewProperties {

    private boolean enabled = true;
    private int minReportThreshold = 5;
    private Duration generateTimeout = Duration.ofSeconds(10);
}
