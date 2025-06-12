package com.example.user.domain.dto.request;

import jakarta.validation.constraints.NotNull;

public record  UserProfileUpdateRequest (
        @NotNull(message="profile is null")
        String profile
) {
}