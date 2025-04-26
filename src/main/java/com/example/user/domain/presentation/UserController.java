package com.example.user.domain.presentation;

import com.example.user.domain.presentation.dto.request.UserNameUpdateRequest;
import com.example.user.domain.presentation.dto.request.UserProfileUpdateRequest;
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

  @GetMapping("/profile")
  public ResponseEntity<UserResponse> findUserById(@RequestHeader("user-id") String userId) {
    UserResponse userResponse = userService.findById(userId);
    return ResponseEntity.ok(userResponse);
  }
  @PatchMapping("/change/profile")
  public ResponseEntity<UserResponse> changeProfileById(@RequestHeader("user-id") String userId, @RequestBody @Valid UserProfileUpdateRequest request) {
    UserResponse userResponse = userService.changeProfileById(userId, request.profile());
    return ResponseEntity.ok(userResponse);
  }
  @PatchMapping("/change/name")
  public ResponseEntity<UserResponse> changeNameById(@RequestHeader("user-id") String userId, @RequestBody @Valid UserNameUpdateRequest request) {
    UserResponse userResponse = userService.changeNameById(userId, request.name());
    return ResponseEntity.ok(userResponse);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteUserById(@RequestHeader("user-id") String userId) {
    userService.deleteById(userId);
  }



}
