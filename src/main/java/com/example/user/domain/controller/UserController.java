package com.example.user.domain.controller;

import com.example.user.domain.dto.request.UserDeleteRequest;
import com.example.user.domain.dto.request.UserNameUpdateRequest;
import com.example.user.domain.dto.response.UserDetailResponse;
import com.example.user.domain.dto.response.UserListResponse;
import com.example.user.domain.dto.response.UserResponse;
import com.example.user.domain.dto.response.UserRoleChangeResponse;
import com.example.user.domain.model.UserRole;
import com.example.user.domain.service.UserService;
import com.example.user.global.dto.ResponseDto;
import com.example.user.global.util.HttpUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Slf4j
public class UserController {
  private final UserService userService;
  private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "name", "remainToken");

  @GetMapping(params = "userIds")
  public ResponseEntity<ResponseDto<List<UserResponse>>> getUsersByIds(@RequestParam("userIds") List<String> userIds) {
    List<UserResponse> userResponses = userService.findAllByIds(userIds);
    ResponseDto<List<UserResponse>> responseDto = HttpUtil.success("get users by ids", userResponses);
    return ResponseEntity.ok(responseDto);
  }

  @GetMapping("/admin")
  public ResponseEntity<ResponseDto<UserListResponse>> listUsers(
          @RequestHeader("role") String requesterRole,
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "20") int size,
          @RequestParam(required = false) String keyword,
          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
          @RequestParam(defaultValue = "createdAt,desc") String sort,
          @RequestParam(required = false, name = "roleFilter") UserRole filterRole
  ) {
    Sort.Direction direction = resolveSortDirection(sort);
    String sortField = resolveSortField(sort);

    UserListResponse userListResponse = userService.listUsers(
            requesterRole,
            page,
            size,
            sortField,
            direction,
            keyword,
            createdFrom,
            createdTo,
            filterRole
    );
    ResponseDto<UserListResponse> responseDto = HttpUtil.success("list users", userListResponse);
    return ResponseEntity.ok(responseDto);
  }

  private Sort.Direction resolveSortDirection(String sort) {
    if (!StringUtils.hasText(sort)) {
      return Sort.Direction.DESC;
    }
    String[] segments = sort.split(",");
    if (segments.length < 2) {
      return Sort.Direction.DESC;
    }
    try {
      return Sort.Direction.fromString(segments[1]);
    } catch (IllegalArgumentException e) {
      return Sort.Direction.DESC;
    }
  }

  private String resolveSortField(String sort) {
    if (!StringUtils.hasText(sort)) {
      return "createdAt";
    }
    String[] segments = sort.split(",");
    String field = segments[0];
    if (!StringUtils.hasText(field) || !ALLOWED_SORT_FIELDS.contains(field)) {
      return "createdAt";
    }
    return field;
  }

  @GetMapping("/profile")
  public ResponseEntity<ResponseDto<UserResponse>> findUserById(@RequestHeader("user-id") String userId) {
    UserResponse userResponse = userService.findById(userId);
    ResponseDto<UserResponse> responseDto = HttpUtil.success("find user", userResponse);
    return ResponseEntity.ok(responseDto);
  }

  @GetMapping("/admin/{userId}")
  public ResponseEntity<ResponseDto<UserDetailResponse>> findUserByAdmin(
          @RequestHeader("user-id") String adminUserId,
          @RequestHeader("role") String role,
          @PathVariable String userId
  ) {
    UserDetailResponse userResponse = userService.findUserAsAdmin(role, userId);
    log.info("Admin user {} retrieved target user {}", adminUserId, userId);
    ResponseDto<UserDetailResponse> responseDto = HttpUtil.success("admin find user", userResponse);
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
  public ResponseEntity<ResponseDto<Void>> deleteUserById(
          @RequestHeader("user-id") String requesterId,
          @RequestHeader("role") String requesterRole,
          @RequestBody(required = false) UserDeleteRequest request
  ) {
    String targetUserId = request != null ? request.userId() : null;
    userService.deleteById(requesterId, requesterRole, targetUserId);
    ResponseDto<Void> responseDto = HttpUtil.success("delete user");
    return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(responseDto);
  }

  @PatchMapping("/role/admin/{userId}")
  public ResponseEntity<ResponseDto<UserRoleChangeResponse>> promoteToAdmin(
          @RequestHeader("user-id") String requesterId,
          @RequestHeader("role") String requesterRole,
          @PathVariable("userId") String userId
  ) {
    UserRoleChangeResponse response = userService.promoteToAdmin(requesterId, requesterRole, userId);
    String message = response.changed() ? "promote admin" : "already admin";
    ResponseDto<UserRoleChangeResponse> responseDto = HttpUtil.success(message, response);
    return ResponseEntity.ok(responseDto);
  }

  @PatchMapping("/role/user/{userId}")
  public ResponseEntity<ResponseDto<UserRoleChangeResponse>> demoteToUser(
          @RequestHeader("user-id") String requesterId,
          @RequestHeader("role") String requesterRole,
          @PathVariable("userId") String userId
  ) {
    UserRoleChangeResponse response = userService.demoteToUser(requesterId, requesterRole, userId);
    ResponseDto<UserRoleChangeResponse> responseDto = HttpUtil.success("demote admin", response);
    return ResponseEntity.ok(responseDto);
  }
}
