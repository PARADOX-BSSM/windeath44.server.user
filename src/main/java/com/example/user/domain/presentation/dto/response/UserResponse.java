package com.example.user.domain.presentation.dto.response;

import com.example.user.domain.domain.User;

public record UserResponse (
        String userId,
        String name,
        Long remain_token,
        String profile
) {
  public static UserResponse toUserResponse(User user) {
    return new UserResponse(user.getUserId(), user.getName(), user.getRemain_token(), user.getProfile());
  }
}
