package com.example.user.domain.controller;

import com.example.user.domain.dto.request.UserCreateRequest;
import com.example.user.global.dto.ResponseDto;
import com.example.user.domain.service.UserService;
import com.example.user.global.util.HttpUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class RegisterController {
  private final UserService userService;

  @PostMapping("/register")
  public ResponseEntity<ResponseDto<Void>> register(@Valid @RequestBody UserCreateRequest request) {
    userService.register(request);
    ResponseDto<Void> responseDto = HttpUtil.success("register user");
    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(responseDto);
  }

  @PostMapping("/register/admin")
  public ResponseEntity<ResponseDto<Void>> registerAdmin(@Valid @RequestBody UserCreateRequest request) {
    userService.registerAdmin(request);
    ResponseDto<Void> responseDto = HttpUtil.success("register admin");
    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(responseDto);
  }
}
