package com.sisibibi.api.domain.report.queue;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai-report.queue")
public class AiReportQueueProperties {

    private String queueUrl;
    private String region = "ap-northeast-2";
    private Retry retry = new Retry();

    @Getter
    @Setter
    public static class Retry {

        private Duration staleThreshold = Duration.ofSeconds(30);
        private int batchSize = 20;
        private int maxRetryCount = 5;
    }
}
