package com.example.user.domain.mapper;

import com.example.user.domain.dto.request.UserCreateRequest;
import com.example.user.domain.dto.response.UserResponse;
import com.example.user.domain.model.User;
import com.example.user.domain.model.UserRole;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel="spring", builder = @Builder(disableBuilder = false))
public interface UserMapper {

  User toEntity(UserCreateRequest request, UserRole role);

  UserResponse toDto(User user);
}