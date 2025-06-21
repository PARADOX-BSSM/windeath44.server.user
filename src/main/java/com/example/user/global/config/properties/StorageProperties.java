package com.example.user.global.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("storage")
public class StorageProperties {
  private String bucketName;
  private String namespaceName;
  private String  configurationFilePath;
  private String profile;
}
