package com.example.user.domain.domain.mapper;

//import com.example.grpc.OauthUserLoginRequest;
import com.example.grpc.OauthUserLoginRequest;
import com.example.user.domain.domain.User;
import com.example.user.domain.domain.UserRole;
import com.example.user.domain.presentation.dto.request.UserCreateRequest;
import com.example.user.domain.presentation.dto.response.UserResponse;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;

@Mapper(componentModel="spring", builder = @Builder(disableBuilder = false))
public interface UserMapper {

  User toEntity(UserCreateRequest request, UserRole role);

  User toEntity(OauthUserLoginRequest request, UserRole role);
  UserResponse toDto(User user);
}
