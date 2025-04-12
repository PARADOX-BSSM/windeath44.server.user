package com.example.user.domain.service;

import com.example.user.domain.domain.User;
import com.example.user.domain.domain.repository.UserRepository;
import com.example.user.domain.exception.AlreadyExistsUserException;
import com.example.user.domain.exception.NotFoundUserException;
import com.example.user.domain.presentation.dto.request.UserCreateRequest;
import com.example.user.domain.presentation.dto.request.UserUpdateRequest;
import com.example.user.domain.presentation.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public void register(UserCreateRequest request) {
    String email = request.email();
    checkExistsUserByEmail(email);
    User user = User.create(
            request.userId(),
            email,
            request.name()
    );
    user.changeToEncodedPassword(request.password(), passwordEncoder);
    System.out.println("Saving : " + user);
    userRepository.save(user);
  }
private void checkExistsUserByEmail(String email) {
  boolean existsUser = userRepository.existsUserByEmail(email);
  if (existsUser) {
    throw new AlreadyExistsUserException("User already exists");
  }
}
  public UserResponse findById(String userId) {
    User user = getUserById(userId);
    UserResponse userResponse = UserResponse.toUserResponse(user);
    return userResponse;
  }

  public UserResponse changeById(String userId, UserUpdateRequest updateInfo) {
    User user = getUserById(userId);
    user.change(updateInfo, passwordEncoder);
    UserResponse userResponse = UserResponse.toUserResponse(user);
    return userResponse;
  }

  private User getUserById(String userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundUserException("Not found user with id"));
    return user;
  }

  public void deleteById(String userId) {
    User user = getUserById(userId);
    userRepository.delete(user);
  }
}
