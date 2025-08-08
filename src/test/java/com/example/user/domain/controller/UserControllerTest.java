package com.example.user.domain.controller;

import com.example.user.domain.dto.response.UserResponse;
import com.example.user.domain.service.UserService;
import com.example.user.global.dto.ResponseDto;
import com.example.user.global.util.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    @DisplayName("유효한 userId로 사용자 조회 시 성공적으로 UserResponse를 반환한다")
    void when_valid_userId_then_findUserById_returns_user_successfully() {
        // Given
        String userId = "test123";
        UserResponse mockUserResponse = mock(UserResponse.class);
        ResponseDto<UserResponse> mockResponseDto = new ResponseDto<>("find user", mockUserResponse);
        
        given(userService.findById(userId)).willReturn(mockUserResponse);
        
        try (MockedStatic<HttpUtil> mockedHttpUtil = mockStatic(HttpUtil.class)) {
            mockedHttpUtil.when(() -> HttpUtil.success("find user", mockUserResponse))
                    .thenReturn(mockResponseDto);
            
            // When
            ResponseEntity<ResponseDto<UserResponse>> response = userController.findUserById(userId);
            log.info("response:{}", response.getBody().data().role());
            
            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(mockResponseDto, response.getBody());
            assertEquals("find user", response.getBody().message());
            assertEquals(mockUserResponse, response.getBody().data());
            
            then(userService).should().findById(userId);
            mockedHttpUtil.verify(() -> HttpUtil.success("find user", mockUserResponse));
        }
    }

    @Test
    @DisplayName("userService.findById가 예외를 던질 때 예외가 전파된다")
    void when_userService_throws_exception_then_findUserById_propagates_exception() {
        // Given
        String userId = "nonexistent";
        RuntimeException expectedException = new RuntimeException("User not found");
        
        given(userService.findById(userId)).willThrow(expectedException);
        
        // When & Then
        RuntimeException actualException = assertThrows(RuntimeException.class,
                () -> userController.findUserById(userId));
        
        assertEquals(expectedException, actualException);
        then(userService).should().findById(userId);
    }

    @Test
    @DisplayName("null userId로 호출해도 userService에 null이 전달된다")
    void when_null_userId_then_findUserById_passes_null_to_service() {
        // Given
        String userId = null;
        UserResponse mockUserResponse = mock(UserResponse.class);
        ResponseDto<UserResponse> mockResponseDto = new ResponseDto<>("find user", mockUserResponse);
        
        given(userService.findById(null)).willReturn(mockUserResponse);
        
        try (MockedStatic<HttpUtil> mockedHttpUtil = mockStatic(HttpUtil.class)) {
            mockedHttpUtil.when(() -> HttpUtil.success("find user", mockUserResponse))
                    .thenReturn(mockResponseDto);
            
            // When
            ResponseEntity<ResponseDto<UserResponse>> response = userController.findUserById(userId);
            
            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            then(userService).should().findById(null);
        }
    }

    @Test
    @DisplayName("빈 문자열 userId로 호출해도 userService에 빈 문자열이 전달된다")
    void when_empty_userId_then_findUserById_passes_empty_string_to_service() {
        // Given
        String userId = "";
        UserResponse mockUserResponse = mock(UserResponse.class);
        ResponseDto<UserResponse> mockResponseDto = new ResponseDto<>("find user", mockUserResponse);
        
        given(userService.findById("")).willReturn(mockUserResponse);
        
        try (MockedStatic<HttpUtil> mockedHttpUtil = mockStatic(HttpUtil.class)) {
            mockedHttpUtil.when(() -> HttpUtil.success("find user", mockUserResponse))
                    .thenReturn(mockResponseDto);
            
            // When
            ResponseEntity<ResponseDto<UserResponse>> response = userController.findUserById(userId);
            
            // Then
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            then(userService).should().findById("");
        }
    }
}