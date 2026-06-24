package com.sisibibi.api.domain.report.queue;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai-report.queue")
public class AiReportQueueProperties {

    private String queueUrl;
    private String region = "ap-northeast-2";
}
