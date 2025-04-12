package com.example.user.domain.presentation;

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
@RequestMapping("/users")
public class UserController {
  private final UserService userService;

  @GetMapping("/{user_id}")
  public ResponseEntity<UserResponse> findUserById(@PathVariable("user_id") String userId) {
    UserResponse userResponse = userService.findById(userId);
    return ResponseEntity.ok(userResponse);
  }
  @PatchMapping("/{user_id}")
  public ResponseEntity<UserResponse> changeUserById(@PathVariable("user_id") String userId, @Valid @RequestBody UserUpdateRequest request) {
    UserResponse userResponse = userService.changeById(userId, request);
    return ResponseEntity.ok(userResponse);
  }
  @DeleteMapping("/{user_id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteUserById(@PathVariable("user_id") String userId) {
    userService.deleteById(userId);
  }
}
