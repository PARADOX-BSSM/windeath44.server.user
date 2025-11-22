package com.example.user.domain.dto.response;

import com.example.user.domain.model.UserRole;

import java.time.LocalDateTime;

public record UserRoleChangeResponse(
        String userId,
        UserRole previousRole,
        UserRole newRole,
        String updatedBy,
        LocalDateTime updatedAt,
        boolean changed
) {
}
