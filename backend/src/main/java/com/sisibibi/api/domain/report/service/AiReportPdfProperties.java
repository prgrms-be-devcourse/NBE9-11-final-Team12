package com.sisibibi.api.domain.report.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai-report.pdf")
public class AiReportPdfProperties {

    private boolean enabled = true;
    private Duration presignedUrlExpiration = Duration.ofMinutes(10);
    private int maxPdfRetryCount = 3;
    private int maxNotificationRetryCount = 5;
    private long retryFixedDelayMs = 60000L;
    private Duration retryStaleThreshold = Duration.ofMinutes(1);
    private int retryBatchSize = 20;
}
