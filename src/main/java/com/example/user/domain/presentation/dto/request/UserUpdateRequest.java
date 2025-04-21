package com.example.user.domain.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record  UserUpdateRequest (
        @NotNull(message="name is null")
        String name,
        @NotNull(message="profile is null")
        String profile
) {
}
