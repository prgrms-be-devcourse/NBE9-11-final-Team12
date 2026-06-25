package com.sisibibi.api.global.storage;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.storage.s3")
public class S3StorageProperties {

  private String region = "ap-northeast-2";
  private String bucket;
  private String publicBaseUrl;
  private Duration presignedUrlExpiration = Duration.ofMinutes(10);
  private long maxImageSize = 5 * 1024 * 1024;
}