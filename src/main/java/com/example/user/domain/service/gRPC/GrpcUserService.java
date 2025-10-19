package com.example.user.domain.service.gRPC;

import com.example.grpc.GetUserRemainTokenRequest;
import com.example.grpc.GetUserRemainTokenResponse;
import com.example.grpc.UserServiceGrpc;
import com.example.user.domain.model.User;
import com.example.user.domain.repository.UserRepository;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class GrpcUserService extends UserServiceGrpc.UserServiceImplBase {
  private final UserRepository userRepository;

  @Override
  public void getUserRemainToken(GetUserRemainTokenRequest request, StreamObserver<GetUserRemainTokenResponse> responseObserver) {
    try {
      String userId = request.getUserId();

      User user = findUserByUserId(userId);

      GetUserRemainTokenResponse response = GetUserRemainTokenResponse.newBuilder()
              .setRemainToken(user.getRemainToken())
              .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();
    } catch (StatusRuntimeException e) {
      responseObserver.onError(e);
    }
  }

  private User findUserByUserId(String userId) {
    return userRepository.findByUserId(userId)
            .orElseThrow(() -> Status.NOT_FOUND
                    .withDescription("Not found user with userId: " + userId)
                    .asRuntimeException()
            );
  }
}
