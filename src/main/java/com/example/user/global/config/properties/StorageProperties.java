package com.example.user.global.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("storage")
public class StorageProperties {
  private String accessKey;
  private String secretKey;
  private String bucketName;
  private String region;
}
