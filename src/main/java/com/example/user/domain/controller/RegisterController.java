package com.example.user.domain.controller;

import com.example.user.global.mapper.ResponseDtoMapper;
import com.example.user.domain.dto.request.UserCreateRequest;
import com.example.user.global.mapper.dto.ResponseDto;
import com.example.user.domain.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RegisterController {
  private final UserService userService;
  private final ResponseDtoMapper responseDtoMapper;

  @PostMapping("/register")
  public ResponseEntity<ResponseDto<Void>> register(@Valid @RequestBody UserCreateRequest request) {
    userService.register(request);
    ResponseDto<Void> responseDto = responseDtoMapper.toResponseDto("register user", null);
    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(responseDto);
  }

}
