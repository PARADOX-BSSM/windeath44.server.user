package com.example.user.global.error.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorCode {
  USER_ID_ALREADY_EXISTS(400, "User Id already exists"),
  USER_EMAIL_ALREADY_EXISTS(400, "User email already exists"),
  USER_NOT_FOUND(404, "User not found"),
  PASSWORD_VALIDATION_FAILED(400, "Password validation failed"),
  FILE_UPLOAD_FAILED(500, "File upload failed");
  private int status;
  private String message;
}
