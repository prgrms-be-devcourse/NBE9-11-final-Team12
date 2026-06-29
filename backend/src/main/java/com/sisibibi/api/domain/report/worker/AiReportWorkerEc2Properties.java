package com.sisibibi.api.domain.report.worker;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai-report.worker.ec2")
public class AiReportWorkerEc2Properties {

    private boolean enabled = false;
    private String region = "ap-northeast-2";
    private String instanceId;
    private String accessKeyId;
    private String secretAccessKey;
    private boolean idleStopEnabled = true;
    private Duration idleTimeout = Duration.ofMinutes(10);
}
