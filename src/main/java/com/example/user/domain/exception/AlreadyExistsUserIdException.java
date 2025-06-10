package com.example.user.domain.exception;

import com.example.user.global.error.exception.ErrorCode;
import com.example.user.global.error.exception.GlobalException;

public class AlreadyExistsUserIdException extends GlobalException {
  public AlreadyExistsUserIdException() {
    super(ErrorCode.USER_ID_ALREADY_EXISTS);
  }

  private static class Holder {
    private static final AlreadyExistsUserIdException INSTANCE = new AlreadyExistsUserIdException();
  }
  public static AlreadyExistsUserIdException getInstance() {
    return Holder.INSTANCE;
  }
 }
