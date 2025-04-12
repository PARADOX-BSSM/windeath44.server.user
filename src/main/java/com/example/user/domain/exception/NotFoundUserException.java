package com.example.user.domain.exception;

public class NotFoundUserException extends RuntimeException {
  public NotFoundUserException(String s) {
    super(s);
  }
}
