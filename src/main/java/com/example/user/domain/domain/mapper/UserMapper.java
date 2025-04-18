package com.example.user.domain.domain.mapper;

import com.example.user.domain.domain.User;
import com.example.user.domain.presentation.dto.request.UserCreateRequest;
import com.example.user.domain.presentation.dto.response.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel="spring")
public interface UserMapper {
  User toEntity(UserCreateRequest request);
  UserResponse toDto(User user);
}
