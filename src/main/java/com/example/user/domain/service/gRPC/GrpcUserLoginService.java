package com.example.user.domain.service.gRPC;

import com.example.grpc.UserLoginRequest;
import com.example.grpc.UserLoginResponse;
import com.example.grpc.UserLoginServiceGrpc;
import com.example.user.domain.domain.User;
import com.example.user.domain.domain.repository.UserRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.crypto.password.PasswordEncoder;

@GrpcService
@RequiredArgsConstructor
public class GrpcUserLoginService extends UserLoginServiceGrpc.UserLoginServiceImplBase {
  private final PasswordEncoder passwordEncoder;
  private final UserRepository userRepository;

  @Override
  public void checkUser(UserLoginRequest request, StreamObserver<UserLoginResponse> responseObserver) {
    try {
      String userId = request.getUserId();
      String password = request.getPassword();

      User user = findUserByUserId(userId);
      boolean existsUser = existsUser(user, password, passwordEncoder);

      UserLoginResponse checkUserResponse = UserLoginResponse.newBuilder()
              .setExistsUser(existsUser)
              .setUserKey(String.valueOf(user.getUserKey()))
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

  private User findUserByUserId(String userId) {
    User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> Status.NOT_FOUND
                    .withDescription("Not found user with email")
                    .asRuntimeException()
            );
    return user;
  }

}
