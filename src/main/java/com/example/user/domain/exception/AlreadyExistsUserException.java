package com.example.user.domain.exception;

public class AlreadyExistsUserException extends RuntimeException {
  public AlreadyExistsUserException(String s) {
    super(s);
  }
}
