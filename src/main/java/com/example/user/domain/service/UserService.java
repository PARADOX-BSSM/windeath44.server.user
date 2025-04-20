package com.example.user.domain.service;

import com.example.user.domain.domain.User;
import com.example.user.domain.domain.UserRole;
import com.example.user.domain.domain.mapper.UserMapper;
import com.example.user.domain.domain.repository.UserRepository;
import com.example.user.domain.exception.AlreadyExistsUserEmailException;
import com.example.user.domain.exception.AlreadyExistsUserException;
import com.example.user.domain.exception.AlreadyExistsUserIdException;
import com.example.user.domain.exception.NotFoundUserException;
import com.example.user.domain.presentation.dto.request.UserCreateRequest;
import com.example.user.domain.presentation.dto.request.UserUpdateRequest;
import com.example.user.domain.presentation.dto.response.UserResponse;
import com.example.user.domain.service.gRPC.GrpcClientService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final GrpcClientService grpcClientService;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  @Transactional
  public void register(UserCreateRequest request) {
    String userId = request.userId();
    String email = request.email();

    grpcClientService.validateEmail(email);
    checkExistsUser(userId, email);
    User user = toUser(request);
    user.changeToEncodedPassword(request.password(), passwordEncoder);
    userRepository.save(user);
  }

  private void checkExistsUser(String userId, String email) {
  boolean existsUserById = userRepository.existsByUserId(userId);
  if (existsUserById) {
    throw new AlreadyExistsUserIdException("UserId already exists");
  }

  boolean existsUserByEmail = userRepository.existsUserByEmail(email);
  if (existsUserByEmail) {
    throw new AlreadyExistsUserEmailException("UserEmail already exists");
  }
}
  public UserResponse findById(String userId) {
    User user = getUserById(userId);
    UserResponse userResponse = toUserResponse(user);
    return userResponse;
  }

  public UserResponse changeById(String userId, UserUpdateRequest updateInfo) {
    User user = getUserById(userId);
    String name = updateInfo.name();
    String profile = updateInfo.profile();
    String password = updateInfo.password();
    user.update(name, profile, password, passwordEncoder);
    UserResponse userResponse = toUserResponse(user);
    return userResponse;
  }

  private User getUserById(String userId) {
    User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new NotFoundUserException("Not found user with id"));
    return user;
  }

  public void deleteById(String userId) {
    User user = getUserById(userId);
    userRepository.delete(user);
  }

  private User toUser(UserCreateRequest request) {
    User user = userMapper.toEntity(request, UserRole.USER);
    return user;

  }

  private UserResponse toUserResponse(User user) {
    return userMapper.toDto(user);
  }

}
