package com.example.user.domain.service.gRPC;

import com.example.grpc.*;
import com.example.user.domain.exception.GrpcMappedException;
import com.example.user.domain.exception.GrpcStatusMapper;
import com.example.user.domain.exception.NotFoundUserException;
import com.example.user.domain.presentation.dto.response.UserResponse;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrpcClientService {
  @GrpcClient("auth-server")
  private UserRegisterServiceGrpc.UserRegisterServiceBlockingStub authenticationServiceBlockingStub;

  public void validateEmail(String email) {
    UserRegisterResponse response = sendToRegisterUserRequest(email);
    boolean userExists = response.getEmailValidation();
  }


  private UserRegisterResponse sendToRegisterUserRequest (String email) {
    UserRegisterRequest request = UserRegisterRequest.newBuilder()
            .setEmail(email)
            .build();
    UserRegisterResponse response = getUserRegisterResponse(request);
    return response;
  }

  private UserRegisterResponse getUserRegisterResponse(UserRegisterRequest request) {
    try {

      UserRegisterResponse response = authenticationServiceBlockingStub.checkEmailValidation(request);

      return response;
    } catch (StatusRuntimeException e) {
      e.printStackTrace();
      throw new GrpcMappedException(e.getStatus().getDescription(), GrpcStatusMapper.resolve(e.getStatus().getCode()));
    }
  }
}