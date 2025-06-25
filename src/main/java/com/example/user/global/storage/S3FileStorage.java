package com.example.user.global.storage;

import com.example.user.global.config.properties.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class S3FileStorage implements FileStorage {
  private final StorageProperties storageProperties;

  public String upload(String objectName, MultipartFile file) throws IOException {
    return putObject(objectName, file);
  }

  private String putObject(String objectName, MultipartFile file) throws IOException {
    String accessKey = storageProperties.getAccessKey();
    String secretKey = storageProperties.getSecretKey();
    String bucketName = storageProperties.getBucketName();
    String region = storageProperties.getRegion();

    AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

    try (S3Client s3 = S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()) {

      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
              .bucket(bucketName)
              .key(objectName)
              .contentType(file.getContentType())
              .build();

      s3.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

      return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + objectName;
    }
  }
}
