package com.sisibibi.api.global.storage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3StorageConfig {

  @Bean
  public S3Client s3Client(S3StorageProperties properties) {
    return S3Client.builder()
        .region(Region.of(properties.getRegion()))
        .build();
  }

  @Bean
  public S3Presigner s3Presigner(S3StorageProperties properties) {
    return S3Presigner.builder()
        .region(Region.of(properties.getRegion()))
        .build();
  }
}