package com.example.user.domain.presentation;

import com.example.user.domain.presentation.dto.request.UserRetrieveRequest;
import com.example.user.domain.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/retrieve")
public class RetrieveUserController {
  private final UserService userService;

  @PatchMapping("/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void changePassword(@RequestBody @Valid UserRetrieveRequest request) {
    userService.changePassword(request.email(), request.password());
  }

  @PostMapping("/userId")
  public ResponseEntity<String> retrieveUserId(@RequestBody @Valid UserRetrieveRequest request) {
    String userId = userService.retrieveUserId(request.email(), request.password());
    return ResponseEntity.ok(userId);
  }

}
