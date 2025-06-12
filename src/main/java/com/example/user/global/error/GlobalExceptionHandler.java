package com.example.user.global.error;

import com.example.user.domain.exception.gRPC.GrpcMappedException;
import com.example.user.global.error.exception.ErrorCode;
import com.example.user.global.error.exception.GlobalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(GlobalException.class)
  public ResponseEntity<ErrorResponse> globalException(GlobalException e) {
    ErrorCode errorCode = e.getErrorCode();
    int status = errorCode.getStatus();
    log.error(errorCode.getMessage());

    return new ResponseEntity<>(new ErrorResponse(errorCode), HttpStatus.valueOf(status));
  }

  @ExceptionHandler(GrpcMappedException.class)
  public ResponseEntity<Void> grpcMappedException(GrpcMappedException e) {
    return ResponseEntity
            .status(e.getStatus())
            .build();
  }

}
