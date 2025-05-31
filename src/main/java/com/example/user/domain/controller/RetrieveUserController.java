package com.example.user.domain.controller;

import com.example.user.global.mapper.ResponseDtoMapper;
import com.example.user.domain.dto.request.UserRetrieveRequest;
import com.example.user.global.mapper.dto.ResponseDto;
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
  private final ResponseDtoMapper responseDtoMapper;

  @PatchMapping("/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public ResponseEntity<ResponseDto<Void>> changePassword(@RequestBody @Valid UserRetrieveRequest request) {
    userService.changePassword(request.email(), request.password());
    ResponseDto<Void> responseDto = responseDtoMapper.toResponseDto("change password", null);
    return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(responseDto);
  }

  @PostMapping("/userId")
  public ResponseEntity<ResponseDto<String>> retrieveUserId(@RequestBody @Valid UserRetrieveRequest request) {
    String userId = userService.retrieveUserId(request.email(), request.password());
    ResponseDto<String> responseDto = responseDtoMapper.toResponseDto("retrieve userId", userId);
    return ResponseEntity.ok(responseDto);
  }

}
