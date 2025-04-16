package com.example.user.domain.service;

import com.example.grpc.AuthenticationServiceGrpc;
import com.example.grpc.CheckUserRequest;
import com.example.grpc.CheckUserResponse;
import com.example.user.domain.domain.User;
import com.example.user.domain.domain.repository.UserRepository;
import com.example.user.domain.exception.NotFoundUserException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.crypto.password.PasswordEncoder;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class AuthenticationGrpcService extends AuthenticationServiceGrpc.AuthenticationServiceImplBase {
  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;

  @Override
  public void checkUser(CheckUserRequest request, StreamObserver<CheckUserResponse> responseObserver) {
    try {
      String email = request.getEmail();
      String password = request.getPassword();
      User user = findUserByEmail(email);
      boolean existsUser = existsUser(user, password, passwordEncoder);

      CheckUserResponse checkUserResponse = CheckUserResponse.newBuilder()
              .setExistsUser(existsUser)
              .setUserId(user.getUserId())
              .setRole(String.valueOf(user.getRole()))
              .build();
      responseObserver.onNext(checkUserResponse);
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      responseObserver.onError(e);
    }
  }

  private boolean existsUser(User user, String password, PasswordEncoder passwordEncoder) {
    return user.equalsPassword(password, passwordEncoder);
  }

  private User findUserByEmail(String email) {
    User user = userRepository.findUserByEmail(email)
            .orElseThrow(() -> Status.NOT_FOUND
                    .withDescription("Not found user with email")
                    .asRuntimeException()
            );
    return user;
  }

}
