package com.example.user.domain.exception;

import com.example.user.global.error.exception.ErrorCode;
import com.example.user.global.error.exception.GlobalException;

public class NotFoundUserException extends GlobalException {
  public NotFoundUserException() {
    super(ErrorCode.USER_NOT_FOUND);
  }
  private static class Holder {
    private static NotFoundUserException INSTANCE = new NotFoundUserException();
  }
  public static NotFoundUserException getInstance() {
    return Holder.INSTANCE;
  }
}
