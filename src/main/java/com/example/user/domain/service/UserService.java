package com.example.user.domain.service;

import com.example.user.domain.model.User;
import com.example.user.domain.model.UserRole;
import com.example.user.domain.mapper.UserMapper;
import com.example.user.domain.repository.UserRepository;
import com.example.user.domain.exception.AlreadyExistsUserEmailException;
import com.example.user.domain.exception.AlreadyExistsUserIdException;
import com.example.user.domain.exception.NotFoundUserException;
import com.example.user.domain.exception.ValidationPasswordException;
import com.example.user.domain.dto.request.UserCreateRequest;
import com.example.user.domain.dto.response.UserResponse;
import com.example.user.domain.service.gRPC.GrpcClientService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

  @Transactional
  public UserResponse changeProfileById(String userId, String profile) {
    User user = getUserById(userId);
    user.updateProfile(profile);
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

  public String retrieveUserId(String email, String password) {
    User user = getUserByEmail(email);
    validatePassword(password, user);
    String userId = user.getUserId();
    return userId;
  }

  private void validatePassword(String password, User user) {
    boolean validatePassword = user.equalsPassword(password, passwordEncoder);
    if (!validatePassword) {
      throw ValidationPasswordException.getInstance();
    }
  }

}