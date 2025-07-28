package com.example.user.domain.dto.response;

public record UserResponse (
        String userId,
        String name,
        Long remainToken,
        String profile
) {
}