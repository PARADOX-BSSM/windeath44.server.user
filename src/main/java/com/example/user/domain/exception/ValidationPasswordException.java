package com.example.user.domain.exception;

import com.example.user.global.error.exception.ErrorCode;
import com.example.user.global.error.exception.GlobalException;

public class ValidationPasswordException extends GlobalException {
  public ValidationPasswordException() {
    super(ErrorCode.PASSWORD_VALIDATION_FAILED);
  }
  private static class Holder {
    private static final ValidationPasswordException INSTANCE = new ValidationPasswordException();
  }
  public static ValidationPasswordException getInstance() {
    return Holder.INSTANCE;
  }

}
