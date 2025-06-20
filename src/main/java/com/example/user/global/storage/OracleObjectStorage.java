package com.example.user.global.storage;

import com.example.user.global.config.properties.StorageProperties;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class OracleObjectStorage implements FileStorage {
  private final StorageProperties storageProperties;

  private String putObject(String objectName, MultipartFile file) throws IOException {
    String configurationFilePath = storageProperties.getConfigurationFilePath();
    String profile = storageProperties.getProfile();
    ConfigFileAuthenticationDetailsProvider provider =
            new ConfigFileAuthenticationDetailsProvider(configurationFilePath, profile);

    try (
            ObjectStorageClient client = new ObjectStorageClient(provider);
            InputStream inputStream = file.getInputStream();
    ) {
      String bucketName = storageProperties.getBucketName();
      String namespaceName = storageProperties.getNamespaceName();

      PutObjectRequest request = buildObjectRequest(file, bucketName, namespaceName, objectName, inputStream);
      client.putObject(request);

      return "https://objectstorage.ap-seoul-1.oraclecloud.com/n/" + namespaceName + "/b/" + bucketName + "/o/" + objectName;
    }
  }

  public String upload(String userId, MultipartFile file) throws IOException {
    return putObject(userId, file);
  }


  private PutObjectRequest buildObjectRequest(MultipartFile file, String bucketName, String namespaceName, String objectName, InputStream inputStream) {
    PutObjectRequest request = PutObjectRequest.builder()
            .bucketName(bucketName)
            .namespaceName(namespaceName)
            .objectName(objectName)
            .putObjectBody(inputStream)
            .contentLength(file.getSize())
            .build();
    return request;
  }
}
