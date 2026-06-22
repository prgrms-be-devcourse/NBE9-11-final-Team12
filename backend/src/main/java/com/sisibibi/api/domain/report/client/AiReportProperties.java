package com.sisibibi.api.domain.report.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai-report")
public class AiReportProperties {

    private String baseUrl;
    private String generatePath = "/report";
    private Duration timeout = Duration.ofSeconds(5);
}
