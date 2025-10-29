package com.example.user.domain.controller;

import com.example.user.domain.dto.request.UserNameUpdateRequest;

import com.example.user.domain.dto.response.UserResponse;
import com.example.user.domain.service.UserService;
import com.example.user.global.dto.ResponseDto;
import com.example.user.global.util.HttpUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
  private final UserService userService;

  @GetMapping
  public ResponseEntity<ResponseDto<List<UserResponse>>> getUsersByIds(@RequestParam("userIds") List<String> userIds) {
    List<UserResponse> userResponsesList = userService.findAllByIds(userIds);
    ResponseDto<List<UserResponse>> responseDto = HttpUtil.success("get users by ids", userResponsesList);
    return ResponseEntity.ok(responseDto);
  }


  @GetMapping("/profile")
  public ResponseEntity<ResponseDto<UserResponse>> findUserById(@RequestHeader("user-id") String userId) {
    UserResponse userResponse = userService.findById(userId);
    ResponseDto<UserResponse> responseDto = HttpUtil.success("find user", userResponse);
    return ResponseEntity.ok(responseDto);
  }

  @PatchMapping("/change/profile")
  public ResponseEntity<ResponseDto<UserResponse>> changeProfileById(@RequestHeader("user-id") String userId, @RequestParam MultipartFile profile) {
    UserResponse userResponse = userService.changeProfileById(userId, profile);
    ResponseDto<UserResponse> responseDto = HttpUtil.success("change profile", userResponse);
    return ResponseEntity.ok(responseDto);
  }

  @PatchMapping("/change/name")
  public ResponseEntity<ResponseDto<UserResponse>> changeNameById(@RequestHeader("user-id") String userId, @RequestBody @Valid UserNameUpdateRequest request) {
    UserResponse userResponse = userService.changeNameById(userId, request.name());
    ResponseDto<UserResponse> responseDto = HttpUtil.success("change name", userResponse);
    return ResponseEntity.ok(responseDto);
  }

  @DeleteMapping
  public ResponseEntity<ResponseDto<Void>> deleteUserById(@RequestHeader("user-id") String userId) {
    userService.deleteById(userId);
    ResponseDto<Void> responseDto = HttpUtil.success("delete user");
    return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(responseDto);
  }

}
