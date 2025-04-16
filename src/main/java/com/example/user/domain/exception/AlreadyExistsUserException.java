package com.example.user.domain.exception;

abstract public class AlreadyExistsUserException extends RuntimeException {
  public AlreadyExistsUserException(String s) {
    super(s);
  }
}
