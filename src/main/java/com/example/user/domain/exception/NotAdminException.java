package com.example.user.domain.exception;

import com.example.user.global.error.exception.ErrorCode;
import com.example.user.global.error.exception.GlobalException;

public class NotAdminException extends GlobalException {
  public NotAdminException() {
    super(ErrorCode.NOT_ADMIN);
  }
  private static class Holder {
    private static NotAdminException INSTANCE = new NotAdminException();
  }
  public static NotAdminException getInstance() {
    return Holder.INSTANCE;
  }
}
