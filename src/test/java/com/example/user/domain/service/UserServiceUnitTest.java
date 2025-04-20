//package com.example.user.domain.service;
//
//import com.example.user.domain.domain.User;
//import com.example.user.domain.domain.mapper.UserMapper;
//import com.example.user.domain.domain.repository.UserRepository;
//import com.example.user.domain.exception.AlreadyExistsUserEmailException;
//import com.example.user.domain.exception.AlreadyExistsUserIdException;
//import com.example.user.domain.exception.NotFoundUserException;
//import com.example.user.domain.presentation.dto.request.UserCreateRequest;
//import com.example.user.domain.presentation.dto.request.UserUpdateRequest;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.util.Optional;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.BDDMockito.given;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.BDDMockito.then;
//import static org.mockito.Mockito.mock;
//
//
//@ExtendWith(MockitoExtension.class)
//public class UserServiceUnitTest {
//  @Mock
//  private UserRepository userRepository;
//  @Mock
//  private PasswordEncoder passwordEncoder;
//
//  @Mock
//  private UserMapper userMapper;
//
//  @InjectMocks
//  private UserService userService;
//
//  @Test
//  @DisplayName("유저가 저장이 되는가?")
//  void when_valid_request_then_register_user_successfully() {
//    UserCreateRequest request = new UserCreateRequest("test", "test123@gmail.com", "방세준", "pw1234");
//    given(userRepository.existsUserByEmail(request.email())).willReturn(false);
//    given(passwordEncoder.encode(request.password())).willReturn("encodedPw");
//
//    userService.register(request);
//
//    then(userRepository).should().save(any(User.class));
//  }
//  @Test
//  @DisplayName("동일한 UserId가 존재하는 경우 예외 발생")
//  void when_valid_userId_then_register_user_failfully() {
//    UserCreateRequest request = new UserCreateRequest("test", "test123@gmail.com", "방세준", "pw1234");
//    given(userRepository.existsById(request.userId())).willReturn(true);
//
//    assertThrows(AlreadyExistsUserIdException.class,
//            () -> userService.register(request));
//  }
//  @Test
//  @DisplayName("동일한 email이 존재하는 경우 예외 발생")
//  void when_valid_email_then_register_user_failfully() {
//    UserCreateRequest request = new UserCreateRequest("test", "test123@gmail.com", "방세준", "pw1234");
//    given(userRepository.existsUserByEmail(request.email())).willReturn(true);
//
//    assertThrows(AlreadyExistsUserEmailException.class,
//            () -> userService.register(request));
//  }
//
//
//  @Test
//  @DisplayName("UserId를 통해 유저를 찾을 수 있는가?")
//  void when_valid_userId_then_findById_user_successfully() {
//    String userId = "test";
//    UserCreateRequest userCreateRequest = new UserCreateRequest(userId, "test123@gmail.com", "pdh", "asdasd");
//    given(userRepository.findById(userId)).willReturn(Optional.ofNullable(userMapper.toEntity(userCreateRequest)));
//
//    userService.findById(userId);
//
//    then(userRepository).should().findById(userId);
//  }
//
//  @Test
//  @DisplayName("UserId를 통해 존재하지 않는 유저를 조회하려 했을 경우 예외 발생")
//  void when_valid_userId_then_findById_user_failfully() {
//    String userId = "test";
//    given(userRepository.findById(userId)).willReturn(Optional.empty());
//
//    assertThrows(NotFoundUserException.class,
//            () -> userService.findById(userId)
//    );
//  }
//
//
//  @Test
//  @DisplayName("UserId를 통해 유저를 변경할 수 있는가?")
//  void when_valid_userId_then_changeById_user_successfully() {
//    String userId = "test";
//    UserUpdateRequest updateInfo = new UserUpdateRequest("RSC", "shine", "shine.png");
//    User user = mock(User.class);
//    given(userRepository.findById(userId)).willReturn(Optional.ofNullable(user));
//
//    userService.changeById(userId, updateInfo);
//
//    then(user).should().update(updateInfo, passwordEncoder);
//  }
//
//  @Test
//  @DisplayName("UserId를 통해 유저를 삭제할 수 있는가?")
//  void when_valid_uesrId_then_deleteById_user_successfully() {
//    String userId = "test";
//    User user = mock(User.class);
//    given(userRepository.findById(userId)).willReturn(Optional.ofNullable(user));
//
//    userService.deleteById(userId);
//
//    then(userRepository).should().delete(user);
//  }
//
//}