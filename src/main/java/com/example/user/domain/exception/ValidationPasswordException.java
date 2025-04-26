package com.example.user.domain.exception;

public class ValidationPasswordException extends RuntimeException {
  public ValidationPasswordException(String s) {
    super(s);
  }
}
