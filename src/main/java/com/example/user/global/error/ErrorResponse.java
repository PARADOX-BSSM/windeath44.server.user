package com.example.user.global.error;

import com.example.user.global.error.Exception.ErrorCode;

public record ErrorResponse(
        int status,
        String message
) {
  public ErrorResponse(ErrorCode errorCode) {
    this(
            errorCode.getStatus(),
            errorCode.getMessage()
    );
  }
}
