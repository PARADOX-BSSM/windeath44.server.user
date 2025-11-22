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
  FILE_UPLOAD_FAILED(500, "File upload failed"),
  REMAIN_TOKEN_INSUFFICIENT(500, "Insufficient token balance"),
  NOT_ADMIN(403, "Admin permission required"),
  ROLE_ALREADY_USER(409, "User already has USER role"),
  LAST_ADMIN_NOT_DEMOTABLE(409, "Last admin cannot be demoted"),
  SYSTEM_ACCOUNT_NOT_DEMOTABLE(400, "System account cannot be demoted");
  private int status;
  private String message;
}
