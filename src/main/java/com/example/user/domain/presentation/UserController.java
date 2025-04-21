package com.example.user.domain.presentation;

import com.example.user.domain.presentation.dto.request.UserChangePasswordRequest;
import com.example.user.domain.presentation.dto.request.UserUpdateRequest;
import com.example.user.domain.presentation.dto.response.UserResponse;
import com.example.user.domain.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{user_id}")
public class UserController {
  private final UserService userService;

  @GetMapping
  public ResponseEntity<UserResponse> findUserById(@PathVariable("user_id") String userId) {
    UserResponse userResponse = userService.findById(userId);
    return ResponseEntity.ok(userResponse);
  }
  @PatchMapping
  public ResponseEntity<UserResponse> changeUserById(@PathVariable("user_id") String userId, @RequestBody @Valid UserUpdateRequest request) {
    UserResponse userResponse = userService.changeById(userId, request);
    return ResponseEntity.ok(userResponse);
  }
  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteUserById(@PathVariable("user_id") String userId) {
    userService.deleteById(userId);
  }

  @PatchMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void changePassword(@PathVariable("user_id") String userId, @RequestBody @Valid UserChangePasswordRequest request) {
    userService.changePassword(userId, request.password());
  }
}
