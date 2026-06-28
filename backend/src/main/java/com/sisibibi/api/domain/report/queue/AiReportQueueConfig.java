package com.sisibibi.api.domain.report.queue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AiReportQueueConfig {

    @Bean
    public SqsClient aiReportSqsClient(AiReportQueueProperties properties) {
        return SqsClient.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }
}
