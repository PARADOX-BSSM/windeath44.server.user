package com.example.user.domain.service.gRPC;

import com.example.grpc.UserIdRetrievalRequest;
import com.example.grpc.UserIdRetrievalResponse;
import com.example.grpc.UserIdRetrievalServiceGrpc;
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
public class GrpcUserIdRetrievalService extends UserIdRetrievalServiceGrpc.UserIdRetrievalServiceImplBase {
  private final UserRepository userRepository;

  @Override
  public void getUserIdByEmail(UserIdRetrievalRequest request, StreamObserver<UserIdRetrievalResponse> responseObserver) {
    try {
      String email = request.getEmail();

      log.info("Received getUserIdByEmail request for email: {}", email);

      // 1. email로 사용자 조회
      User user = findUserByEmail(email);

      // 2. 응답 생성
      UserIdRetrievalResponse response = UserIdRetrievalResponse.newBuilder()
              .setUserId(user.getUserId())
              .build();

      log.info("Successfully retrieved userId: {} for email: {}", user.getUserId(), email);

      // 3. 응답 전송
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (StatusRuntimeException e) {
      log.error("StatusRuntimeException occurred while retrieving userId: {}", e.getMessage());
      responseObserver.onError(e);
    } catch (Exception e) {
      log.error("Unexpected error occurred while retrieving userId", e);
      responseObserver.onError(
              Status.INTERNAL
                      .withDescription("사용자 조회 중 오류가 발생했습니다.")
                      .withCause(e)
                      .asRuntimeException()
      );
    }
  }

  private User findUserByEmail(String email) {
    return userRepository.findUserByEmail(email)
            .orElseThrow(() -> {
              log.warn("User not found with email: {}", email);
              return Status.NOT_FOUND
                      .withDescription("해당 이메일의 사용자를 찾을 수 없습니다: " + email)
                      .asRuntimeException();
            });
  }
}
