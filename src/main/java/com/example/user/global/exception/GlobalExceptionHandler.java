package com.example.user.global.exception;

import com.example.user.domain.exception.AlreadyExistsUserException;
import com.example.user.domain.exception.NotFoundUserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(AlreadyExistsUserException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public void alreadyExistsUserException(AlreadyExistsUserException e) {
    log.error(e.getMessage());
  }

  @ExceptionHandler(NotFoundUserException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public void notFoundUserException(NotFoundUserException e) {
    log.error(e.getMessage());
  }

}
