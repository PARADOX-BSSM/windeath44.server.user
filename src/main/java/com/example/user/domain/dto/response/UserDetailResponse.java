package com.example.user.domain.dto.response;

import java.time.LocalDateTime;

public record UserDetailResponse(
        String userId,
        String email,
        String name,
        Long remainToken,
        String profile,
        String role,
        LocalDateTime createdAt
) {
}
