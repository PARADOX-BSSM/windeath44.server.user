package com.example.user.global.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorage {
  String upload(String userId, MultipartFile file) throws IOException;

}
