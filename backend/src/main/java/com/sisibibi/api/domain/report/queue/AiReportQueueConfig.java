package com.sisibibi.api.domain.report.queue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AiReportQueueConfig {

    @Bean
    public SqsClient aiReportSqsClient(AiReportQueueProperties properties) {
        return SqsClient.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(credentialsProvider(properties))
                .build();
    }

    private AwsCredentialsProvider credentialsProvider(AiReportQueueProperties properties) {
        if (StringUtils.hasText(properties.getAccessKeyId())
                && StringUtils.hasText(properties.getSecretAccessKey())) {
            if (StringUtils.hasText(properties.getSessionToken())) {
                return StaticCredentialsProvider.create(AwsSessionCredentials.create(
                        properties.getAccessKeyId(),
                        properties.getSecretAccessKey(),
                        properties.getSessionToken()
                ));
            }

            return StaticCredentialsProvider.create(AwsBasicCredentials.create(
                    properties.getAccessKeyId(),
                    properties.getSecretAccessKey()
            ));
        }

        return DefaultCredentialsProvider.create();
    }
}
