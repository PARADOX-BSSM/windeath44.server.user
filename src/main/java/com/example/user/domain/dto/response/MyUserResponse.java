package com.example.user.domain.dto.response;

public record MyUserResponse(
        String userId,
        String name,
        Long remainToken,
        String profile,
        String role
) {
}
