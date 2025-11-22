package com.example.user.domain.service;

import com.example.user.domain.dto.request.UserCreateRequest;
import com.example.user.domain.dto.response.UserDetailResponse;
import com.example.user.domain.dto.response.UserIdResponse;
import com.example.user.domain.dto.response.UserListResponse;
import com.example.user.domain.dto.response.UserResponse;
import com.example.user.domain.dto.response.UserRoleChangeResponse;
import com.example.user.domain.dto.response.UsersByIdsResponse;
import com.example.user.domain.exception.*;
import com.example.user.domain.mapper.UserMapper;
import com.example.user.domain.model.User;
import com.example.user.domain.model.UserRole;
import com.example.user.domain.repository.UserRepository;
import com.example.user.domain.repository.UserSpecifications;
import com.example.user.domain.service.gRPC.GrpcClientService;
import com.example.user.global.storage.FileStorage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final GrpcClientService grpcClientService;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;
  private final FileStorage fileStorage;
  private static final String SYSTEM_ACCOUNT_PREFIX = "service-account-";

  public UsersByIdsResponse findAllByIds(List<String> userIds) {
    List<User> users = userRepository.findByUserIds(userIds);
    List<UserDetailResponse> userResponseList = users.stream()
            .map(userMapper::toDetailDto)
            .toList();

    Set<String> foundUserIds = users.stream()
            .map(User::getUserId)
            .collect(Collectors.toSet());

    List<String> notFoundUserIds = userIds.stream()
            .filter(id -> !foundUserIds.contains(id))
            .toList();

    return new UsersByIdsResponse(userResponseList, notFoundUserIds);
  }

  public UserListResponse listUsers(
          String requesterRole,
          int page,
          int size,
          String sortField,
          Sort.Direction direction,
          String keyword,
          LocalDateTime createdFrom,
          LocalDateTime createdTo,
          UserRole filterRole
  ) {
    validateAdminRole(requesterRole);

    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 100);
    Sort sort = Sort.by(direction, sortField);
    Pageable pageable = PageRequest.of(safePage, safeSize, sort);

    Specification<User> specification = Specification.where(UserSpecifications.keyword(keyword))
            .and(UserSpecifications.hasRole(filterRole))
            .and(UserSpecifications.createdFrom(createdFrom))
            .and(UserSpecifications.createdTo(createdTo));

    Page<User> userPage = userRepository.findAll(specification, pageable);
    long totalUserCount = userRepository.count();
    List<UserDetailResponse> content = userPage.getContent().stream()
            .map(userMapper::toDetailDto)
            .toList();

    return new UserListResponse(
            content,
            userPage.getNumber(),
            userPage.getSize(),
            userPage.getTotalElements(),
            userPage.getTotalPages(),
            totalUserCount
    );
  }


  @Transactional
  public void register(UserCreateRequest request) {
    registerUser(request, UserRole.USER);
  }

  private void checkExistsUser(String userId, String email) {
    boolean existsUserById = userRepository.existsByUserId(userId);
    if (existsUserById) {
      throw AlreadyExistsUserIdException.getInstance();
    }

    boolean existsUserByEmail = userRepository.existsUserByEmail(email);
    if (existsUserByEmail) {
      throw AlreadyExistsUserEmailException.getInstance();
    }
  }

  public UserResponse findById(String userId) {
    User user = getUserById(userId);
    UserResponse userResponse = toUserResponse(user);
    return userResponse;
  }

  public UserDetailResponse findUserAsAdmin(String requesterRole, String targetUserId) {
    validateAdminRole(requesterRole);
    User user = getUserById(targetUserId);
    return toUserDetailResponse(user);
  }

  @Transactional
  public UserResponse changeProfileById(String userId, MultipartFile profile) {
    User user = getUserById(userId);
    String imageUrl;
    try {
      imageUrl = fileStorage.upload(userId, profile);
    } catch (IOException e) {
      throw FailedUploadFileException.getInstance();
    }
    user.updateProfile(imageUrl);
    UserResponse userResponse = toUserResponse(user);
    return userResponse;
  }

  @Transactional
  public UserResponse changeNameById(String userId, String name) {
    User user = getUserById(userId);
    user.updateName(name);
    UserResponse userResponse = toUserResponse(user);
    return userResponse;
  }

  private User getUserById(String userId) {
    User user = userRepository.findByUserId(userId)
            .orElseThrow(NotFoundUserException::getInstance);
    
    return user;
  }

  public void deleteById(String requesterId, String requesterRole, String targetUserId) {
    String targetId = StringUtils.hasText(targetUserId) ? targetUserId : requesterId;

    if (!requesterId.equals(targetId)) {
      validateAdminRole(requesterRole);
    }

    User user = getUserById(targetId);
    userRepository.delete(user);
  }

  @Transactional
  public UserRoleChangeResponse promoteToAdmin(
          String requesterId,
          String requesterRole,
          String userId
  ) {
    validateAdminRole(requesterRole);
    User user = getUserById(userId);
    UserRole previousRole = user.getRole();
    boolean changed = !UserRole.ADMIN.equals(previousRole);
    if (changed) {
      user.updateRole(UserRole.ADMIN);
    }
    return buildRoleChangeResponse(user, previousRole, requesterId, changed);
  }

  @Transactional
  public UserRoleChangeResponse demoteToUser(
          String requesterId,
          String requesterRole,
          String userId
  ) {
    validateAdminRole(requesterRole);
    validateSystemAccount(userId);

    User targetUser = getUserById(userId);
    if (!targetUser.isAdmin()) {
      throw AlreadyUserRoleException.getInstance();
    }

    long adminCount = userRepository.countByRole(UserRole.ADMIN);
    if (adminCount <= 1) {
      throw LastAdminNotDemotableException.getInstance();
    }

    UserRole previousRole = targetUser.getRole();
    targetUser.updateRole(UserRole.USER);
    return buildRoleChangeResponse(targetUser, previousRole, requesterId, true);
  }

  private static void validateSystemAccount(String targetUserId) {
    if (targetUserId != null && targetUserId.startsWith(SYSTEM_ACCOUNT_PREFIX)) {
      throw SystemAccountNotDemotableException.getInstance();
    }
  }

  private UserRoleChangeResponse buildRoleChangeResponse(
          User user,
          UserRole previousRole,
          String requesterId,
          boolean changed
  ) {
    return new UserRoleChangeResponse(
            user.getUserId(),
            previousRole,
            user.getRole(),
            requesterId,
            LocalDateTime.now(),
            changed
    );
  }

  private User toUser(UserCreateRequest request, UserRole role) {
    User user = userMapper.toEntity(request, role);
    return user;
  }

  private UserResponse toUserResponse(User user) {
    return userMapper.toDto(user);
  }

  private UserDetailResponse toUserDetailResponse(User user) {
    return userMapper.toDetailDto(user);
  }

  private void validateAdminRole(String role) {
    if (!StringUtils.hasText(role)) {
      throw NotAdminException.getInstance();
    }

    UserRole requesterRole;
    try {
      requesterRole = UserRole.valueOf(role.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw NotAdminException.getInstance();
    }

    if (!UserRole.ADMIN.equals(requesterRole)) {
      throw NotAdminException.getInstance();
    }
  }

  @Transactional
  public void changePassword(String email, String password) {
    User user = getUserByEmail(email);
    user.changeToEncodedPassword(password, passwordEncoder);
  }


  private User getUserByEmail(String email) {
    User user = userRepository.findUserByEmail(email)
                    .orElseThrow(NotFoundUserException::getInstance);
    return user;
  }

  public UserIdResponse retrieveUserId(String email, String password) {
    User user = getUserByEmail(email);
    validatePassword(password, user);
    UserIdResponse userIdResponse = userMapper.toUserIdResponse(user);
    return userIdResponse;
  }

  private void validatePassword(String password, User user) {
    boolean validatePassword = user.equalsPassword(password, passwordEncoder);
    if (!validatePassword) {
      throw ValidationPasswordException.getInstance();
    }
  }

  @Transactional
  public void registerAdmin(String userId, UserCreateRequest request) {
    User user = getUserById(userId);
    validateAdmin(user);
    registerUser(request, UserRole.ADMIN);
  }

  private static void validateAdmin(User user) {
    boolean isAdmin = user.isAdmin();
    if (!isAdmin) throw NotAdminException.getInstance();
  }

  private void registerUser(UserCreateRequest request, UserRole role) {
    String userId = request.userId();
    String email = request.email();

    grpcClientService.validateEmail(email);
    checkExistsUser(userId, email);
    User user = toUser(request, role);
    user.changeToEncodedPassword(request.password(), passwordEncoder);
    userRepository.save(user);
  }
}
