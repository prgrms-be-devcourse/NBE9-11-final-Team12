package com.sisibibi.api.domain.report.prompt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.prompt-guard")
public class PromptGuardProperties {

    private String baseUrl;
    private String scanPath = "/scan";
    private Duration timeout = Duration.ofSeconds(2);
    private boolean failOpen = false;
    private int customPromptMaxCount = 5;
    private int customPromptMaxLength = 1000;
}
