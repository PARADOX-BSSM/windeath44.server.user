package com.example.user.domain.exception;

import com.example.user.global.error.exception.GlobalException;
import com.example.user.global.error.exception.ErrorCode;

public class AlreadyExistsUserEmailException extends GlobalException {
  public AlreadyExistsUserEmailException() {
    super(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
  }

  private static class Holder {
    private static final AlreadyExistsUserEmailException INSTANCE = new AlreadyExistsUserEmailException();
  }

  public static AlreadyExistsUserEmailException getInstance() {
    return Holder.INSTANCE;
  }
}
