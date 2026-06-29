package com.sisibibi.api.domain.report.worker;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

@Configuration
public class AiReportWorkerEc2Config {

    @Bean
    public Ec2Client aiReportWorkerEc2Client(AiReportWorkerEc2Properties properties) {
        var builder = Ec2Client.builder()
                .region(Region.of(properties.getRegion()));

        if (StringUtils.hasText(properties.getAccessKeyId())
                && StringUtils.hasText(properties.getSecretAccessKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                            properties.getAccessKeyId(),
                            properties.getSecretAccessKey()
                    )
            ));
        }

        return builder.build();
    }
}
