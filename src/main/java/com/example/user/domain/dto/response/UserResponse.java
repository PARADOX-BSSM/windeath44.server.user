package com.example.user.domain.dto.response;

import com.example.user.domain.model.type.LevelTitle;

public record UserResponse (
        String userId,
        String name,
        Long remainToken,
        String profile,
        String role,
        Long xp,
        int level,
        LevelTitle levelTitle,
        Long nextLevelRequireXp
) {
}
