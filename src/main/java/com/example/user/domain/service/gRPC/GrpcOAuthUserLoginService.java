package com.example.user.domain.service.gRPC;

import com.example.grpc.OauthUserLoginRequest;
import com.example.grpc.OauthUserLoginResponse;
import com.example.grpc.OauthUserLoginServiceGrpc;
import com.example.user.domain.domain.User;
import com.example.user.domain.domain.UserRole;
import com.example.user.domain.domain.mapper.UserMapper;
import com.example.user.domain.domain.repository.UserRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class GrpcOAuthUserLoginService extends OauthUserLoginServiceGrpc.OauthUserLoginServiceImplBase {
  private final UserRepository userRepository;
  private final UserMapper userMapper;
  @Override
  public void oauthUserRegister(OauthUserLoginRequest request, StreamObserver<OauthUserLoginResponse> responseObserver) {
    String email = request.getEmail();
    User user = getOrSave(request, email);
    Long userKey = user.getUserKey();

    OauthUserLoginResponse oauthUserRegisterResponse = OauthUserLoginResponse.newBuilder()
            .setUserKey(String.valueOf(userKey))
            .build();
    responseObserver.onNext(oauthUserRegisterResponse);
    responseObserver.onCompleted();
  }

  private User getOrSave(OauthUserLoginRequest request, String email) {
    return userRepository.findUserByEmail(email)
            .orElseGet(() -> createAndSaveUser(request));
  }

  private User createAndSaveUser(OauthUserLoginRequest request) {
    User user = userMapper.toEntity(request, UserRole.USER);
    userRepository.save(user);
    return user;
  }


}
