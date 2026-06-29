package com.sisibibi.api.domain.report.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai-report.notification")
public class AiReportNotificationProperties {

    private String provider = "smtp";
    private String fromEmail = "";
    private String homepageUrl = "http://localhost:3000";
}
