package com.example.user.domain.service.exception;

import com.example.user.global.error.Exception.ErrorCode;
import com.example.user.global.error.Exception.GlobalException;

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
