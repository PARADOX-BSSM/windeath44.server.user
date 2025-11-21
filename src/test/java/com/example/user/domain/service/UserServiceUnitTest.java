package com.example.user.domain.service;

import com.example.user.domain.dto.response.UserIdResponse;
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
import com.example.user.global.storage.FileStorage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;


@ExtendWith(MockitoExtension.class)
public class UserServiceUnitTest {
  @Mock
  private UserRepository userRepository;
  @Mock
  private PasswordEncoder passwordEncoder;
  @Mock
  private UserMapper userMapper;
  @Mock
  private GrpcClientService grpcClientService;
  @Mock
  private FileStorage fileStorage;

  @InjectMocks
  private UserService userService;

  @Test
  @DisplayName("유저가 저장이 되는가?")
  void when_valid_request_then_register_user_successfully() {
    // Given
    UserCreateRequest request = new UserCreateRequest("test", "test123@gmail.com", "방세준", "pw1234");
    User user = mock(User.class);
    given(userRepository.existsByUserId(request.userId())).willReturn(false);
    given(userRepository.existsUserByEmail(request.email())).willReturn(false);
    given(userMapper.toEntity(request, UserRole.USER)).willReturn(user);
    willDoNothing().given(grpcClientService).validateEmail(request.email());

    // When
    userService.register(request);

    // Then
    then(user).should().changeToEncodedPassword(request.password(), passwordEncoder);
    then(userRepository).should().save(user);
  }

  @Test
  @DisplayName("동일한 UserId가 존재하는 경우 예외 발생")
  void when_valid_userId_then_register_user_failfully() {
    // Given
    UserCreateRequest request = new UserCreateRequest("test", "test123@gmail.com", "방세준", "pw1234");
    given(userRepository.existsByUserId(request.userId())).willReturn(true);
    willDoNothing().given(grpcClientService).validateEmail(request.email());

    // When & Then
    assertThrows(AlreadyExistsUserIdException.class,
            () -> userService.register(request));
  }

  @Test
  @DisplayName("동일한 email이 존재하는 경우 예외 발생")
  void when_valid_email_then_register_user_failfully() {
    // Given
    UserCreateRequest request = new UserCreateRequest("test", "test123@gmail.com", "방세준", "pw1234");
    given(userRepository.existsByUserId(request.userId())).willReturn(false);
    given(userRepository.existsUserByEmail(request.email())).willReturn(true);
    willDoNothing().given(grpcClientService).validateEmail(request.email());

    // When & Then
    assertThrows(AlreadyExistsUserEmailException.class,
            () -> userService.register(request));
  }

  @Test
  @DisplayName("UserId를 통해 유저를 찾을 수 있는가?")
  void when_valid_userId_then_findById_user_successfully() {
    // Given
    String userId = "test";
    User user = mock(User.class);
    UserResponse userResponse = mock(UserResponse.class);
    given(userRepository.findByUserId(userId)).willReturn(Optional.of(user));
    given(userMapper.toDto(user)).willReturn(userResponse);

    // When
    UserResponse result = userService.findById(userId);

    // Then
    assertEquals(userResponse, result);
    then(userRepository).should().findByUserId(userId);
    then(userMapper).should().toDto(user);
  }

  @Test
  @DisplayName("UserId를 통해 존재하지 않는 유저를 조회하려 했을 경우 예외 발생")
  void when_valid_userId_then_findById_user_failfully() {
    // Given
    String userId = "test";
    given(userRepository.findByUserId(userId)).willReturn(Optional.empty());

    // When & Then
    assertThrows(NotFoundUserException.class,
            () -> userService.findById(userId)
    );
  }

  @Test
  @DisplayName("UserId를 통해 유저의 프로필을 변경할 수 있는가?")
  void when_valid_userId_then_changeProfileById_user_successfully() throws IOException {
    // Given
    String userId = "test";
    MultipartFile profile = mock(MultipartFile.class);
    User user = mock(User.class);
    UserResponse userResponse = mock(UserResponse.class);
    given(userRepository.findByUserId(userId)).willReturn(Optional.of(user));
    given(fileStorage.upload(userId, profile)).willReturn("profile-url");
    given(userMapper.toDto(user)).willReturn(userResponse);

    // When
    UserResponse result = userService.changeProfileById(userId, profile);

    // Then
    assertEquals(userResponse, result);
    then(user).should().updateProfile("profile-url");
    then(userMapper).should().toDto(user);
  }

  @Test
  @DisplayName("UserId를 통해 유저의 이름을 변경할 수 있는가?")
  void when_valid_userId_then_changeNameById_user_successfully() {
    // Given
    String userId = "test";
    String name = "new_name";
    User user = mock(User.class);
    UserResponse userResponse = mock(UserResponse.class);
    given(userRepository.findByUserId(userId)).willReturn(Optional.of(user));
    given(userMapper.toDto(user)).willReturn(userResponse);

    // When
    UserResponse result = userService.changeNameById(userId, name);

    // Then
    assertEquals(userResponse, result);
    then(user).should().updateName(name);
    then(userMapper).should().toDto(user);
  }

  @Test
  @DisplayName("UserId를 통해 유저를 삭제할 수 있는가?")
  void when_valid_userId_then_deleteById_user_successfully() {
    // Given
    String userId = "test";
    User user = mock(User.class);
    given(userRepository.findByUserId(userId)).willReturn(Optional.of(user));

    // When
    userService.deleteById(userId);

    // Then
    then(userRepository).should().delete(user);
  }

  @Test
  @DisplayName("이메일을 통해 비밀번호를 변경할 수 있는가?")
  void when_valid_email_then_changePassword_successfully() {
    // Given
    String email = "test@example.com";
    String password = "newPassword";
    User user = mock(User.class);
    given(userRepository.findUserByEmail(email)).willReturn(Optional.of(user));

    // When
    userService.changePassword(email, password);

    // Then
    then(user).should().changeToEncodedPassword(password, passwordEncoder);
  }

  @Test
  @DisplayName("존재하지 않는 이메일로 비밀번호 변경 시 예외 발생")
  void when_invalid_email_then_changePassword_throws_exception() {
    // Given
    String email = "nonexistent@example.com";
    String password = "newPassword";
    given(userRepository.findUserByEmail(email)).willReturn(Optional.empty());

    // When & Then
    assertThrows(NotFoundUserException.class,
            () -> userService.changePassword(email, password));
  }

  @Test
  @DisplayName("이메일과 비밀번호로 유저 ID를 조회할 수 있는가?")
  void when_valid_email_and_password_then_retrieveUserId_successfully() {
    // Given
    String email = "test@example.com";
    String password = "validPassword";
    UserIdResponse userId = new UserIdResponse("testUser");
    User user = mock(User.class);
    given(userRepository.findUserByEmail(email)).willReturn(Optional.of(user));
    given(user.equalsPassword(password, passwordEncoder)).willReturn(true);
    given(userMapper.toUserIdResponse(user)).willReturn(userId);

    // When
    UserIdResponse result = userService.retrieveUserId(email, password);

    // Then
    assertEquals(userId, result);
  }

  @Test
  @DisplayName("존재하지 않는 이메일로 유저 ID 조회 시 예외 발생")
  void when_invalid_email_then_retrieveUserId_throws_exception() {
    // Given
    String email = "nonexistent@example.com";
    String password = "password";
    given(userRepository.findUserByEmail(email)).willReturn(Optional.empty());

    // When & Then
    assertThrows(NotFoundUserException.class,
            () -> userService.retrieveUserId(email, password));
  }

  @Test
  @DisplayName("잘못된 비밀번호로 유저 ID 조회 시 예외 발생")
  void when_invalid_password_then_retrieveUserId_throws_exception() {
    // Given
    String email = "test@example.com";
    String password = "invalidPassword";
    User user = mock(User.class);
    given(userRepository.findUserByEmail(email)).willReturn(Optional.of(user));
    given(user.equalsPassword(password, passwordEncoder)).willReturn(false);

    // When & Then
    assertThrows(ValidationPasswordException.class,
            () -> userService.retrieveUserId(email, password));
  }
}
