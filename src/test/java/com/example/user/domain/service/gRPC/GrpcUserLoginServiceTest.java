package com.example.user.domain.service.gRPC;

import com.example.grpc.UserLoginRequest;
import com.example.grpc.UserLoginResponse;
import com.example.user.domain.model.User;
import com.example.user.domain.model.UserRole;
import com.example.user.domain.repository.UserRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GrpcUserLoginServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StreamObserver<UserLoginResponse> responseObserver;

    @InjectMocks
    private GrpcUserLoginService grpcUserLoginService;

    @Captor
    private ArgumentCaptor<UserLoginResponse> responseCaptor;

    @Test
    @DisplayName("유효한 사용자 ID와 비밀번호로 로그인 성공")
    void when_valid_userId_and_password_then_login_successfully() {
        // Given
        String userId = "testUser";
        String password = "password123";
        UserLoginRequest request = UserLoginRequest.newBuilder()
                .setUserId(userId)
                .setPassword(password)
                .build();

        User user = mock(User.class);
        given(userRepository.findByUserId(userId)).willReturn(Optional.of(user));
        given(user.equalsPassword(password, passwordEncoder)).willReturn(true);
        given(user.getUserId()).willReturn(userId);
        given(user.getRole()).willReturn(UserRole.USER);

        // When
        grpcUserLoginService.checkUser(request, responseObserver);

        // Then
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        UserLoginResponse response = responseCaptor.getValue();
        assertTrue(response.getExistsUser());
        assertEquals(userId, response.getUserId());
        assertEquals(UserRole.USER.toString(), response.getRole());
    }

    @Test
    @DisplayName("유효한 사용자 ID와 잘못된 비밀번호로 로그인 실패")
    void when_valid_userId_and_invalid_password_then_login_fails() {
        // Given
        String userId = "testUser";
        String password = "wrongPassword";
        UserLoginRequest request = UserLoginRequest.newBuilder()
                .setUserId(userId)
                .setPassword(password)
                .build();

        User user = mock(User.class);
        given(userRepository.findByUserId(userId)).willReturn(Optional.of(user));
        given(user.equalsPassword(password, passwordEncoder)).willReturn(false);
        given(user.getUserId()).willReturn(userId);
        given(user.getRole()).willReturn(UserRole.USER);

        // When
        grpcUserLoginService.checkUser(request, responseObserver);

        // Then
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();
        verify(responseObserver, never()).onError(any());

        UserLoginResponse response = responseCaptor.getValue();
        assertFalse(response.getExistsUser());
        assertEquals(userId, response.getUserId());
        assertEquals(UserRole.USER.toString(), response.getRole());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 ID로 로그인 시 예외 발생")
    void when_invalid_userId_then_login_throws_exception() {
        // Given
        String userId = "nonexistentUser";
        String password = "password123";
        UserLoginRequest request = UserLoginRequest.newBuilder()
                .setUserId(userId)
                .setPassword(password)
                .build();

        StatusRuntimeException exception = Status.NOT_FOUND
                .withDescription("Not found user with email")
                .asRuntimeException();
        given(userRepository.findByUserId(userId)).willReturn(Optional.empty());

        // When
        grpcUserLoginService.checkUser(request, responseObserver);

        // Then
        verify(responseObserver).onError(any(StatusRuntimeException.class));
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    @DisplayName("gRPC 요청 처리 중 예외 발생 시 에러 전달")
    void when_exception_occurs_during_processing_then_error_is_propagated() {
        // Given
        String userId = "testUser";
        String password = "password123";
        UserLoginRequest request = UserLoginRequest.newBuilder()
                .setUserId(userId)
                .setPassword(password)
                .build();

        StatusRuntimeException exception = new StatusRuntimeException(Status.INTERNAL);
        given(userRepository.findByUserId(userId)).willThrow(exception);

        // When
        grpcUserLoginService.checkUser(request, responseObserver);

        // Then
        verify(responseObserver).onError(exception);
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
    }
}