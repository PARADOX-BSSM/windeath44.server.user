package com.example.user.domain.presentation;

import com.example.user.domain.presentation.dto.request.UserCreateRequest;
import com.example.user.domain.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RegisterController {
  private final UserService userService;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void register(@Valid @RequestBody UserCreateRequest request) {
    userService.register(request);
  }

}
