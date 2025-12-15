package com.example.user.domain.dto.response;

import com.example.user.domain.model.type.Level;

public record UserResponse (
        String userId,
        String name,
        Long remainToken,
        String profile,
        String role,
        Long xp,
        Level level
) {
}
