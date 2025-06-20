package com.example.user.domain.exception;

import com.example.user.global.error.exception.ErrorCode;
import com.example.user.global.error.exception.GlobalException;

public class FailedUploadFileException extends GlobalException {

  public FailedUploadFileException() {
    super(ErrorCode.FILE_UPLOAD_FAILED);
  }

  private static class Holder {
    private static final FailedUploadFileException INSTANCE = new FailedUploadFileException();
  }
  public static FailedUploadFileException getInstance() {
    return Holder.INSTANCE;
  }
}
