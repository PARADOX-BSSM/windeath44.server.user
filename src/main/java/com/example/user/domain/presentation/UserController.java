package com.example.user.domain.presentation;

import com.example.user.domain.presentation.dto.ResponseDtoMapper;
import com.example.user.domain.presentation.dto.request.UserNameUpdateRequest;
import com.example.user.domain.presentation.dto.request.UserProfileUpdateRequest;
import com.example.user.domain.presentation.dto.response.ResponseDto;
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
  private final ResponseDtoMapper responseDtoMapper;

  @GetMapping("/profile")
  public ResponseEntity<ResponseDto<UserResponse>> findUserById(@RequestHeader("user-id") String userId) {
    UserResponse userResponse = userService.findById(userId);
    ResponseDto<UserResponse> responseDto = responseDtoMapper.toResponseDto("find user", userResponse);
    return ResponseEntity.ok(responseDto);
  }

  @PatchMapping("/change/profile")
  public ResponseEntity<ResponseDto<UserResponse>> changeProfileById(@RequestHeader("user-id") String userId, @RequestBody @Valid UserProfileUpdateRequest request) {
    UserResponse userResponse = userService.changeProfileById(userId, request.profile());
    ResponseDto<UserResponse> responseDto = responseDtoMapper.toResponseDto("change profile", userResponse);
    return ResponseEntity.ok(responseDto);
  }
  @PatchMapping("/change/name")
  public ResponseEntity<ResponseDto<UserResponse>> changeNameById(@RequestHeader("user-id") String userId, @RequestBody @Valid UserNameUpdateRequest request) {
    UserResponse userResponse = userService.changeNameById(userId, request.name());
    ResponseDto<UserResponse> responseDto = responseDtoMapper.toResponseDto("change name", userResponse);
    return ResponseEntity.ok(responseDto);
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<ResponseDto<Void>> deleteUserById(@RequestHeader("user-id") String userId) {
    userService.deleteById(userId);
    ResponseDto<Void> responseDto = responseDtoMapper.toResponseDto("delete user", null);
    return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(responseDto);
  }



}
