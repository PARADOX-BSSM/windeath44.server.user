package com.example.user.domain.service;

import com.example.user.domain.domain.User;
import com.example.user.domain.domain.repository.UserRepository;
import com.example.user.domain.exception.AlreadyExistsUserEmailException;
import com.example.user.domain.exception.AlreadyExistsUserException;
import com.example.user.domain.exception.AlreadyExistsUserIdException;
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
    String userId = request.userId();
    String email = request.email();
    checkExistsUser(userId, email);

    User user = User.create(
            request.userId(),
            email,
            request.name()
    );
    user.changeToEncodedPassword(request.password(), passwordEncoder);
    userRepository.save(user);
  }
private void checkExistsUser(String userId, String email) {
  boolean existsUserById = userRepository.existsById(userId);
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
