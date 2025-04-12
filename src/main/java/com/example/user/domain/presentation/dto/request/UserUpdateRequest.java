package com.example.user.domain.presentation.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UserUpdateRequest (
        @NotEmpty
        String name,
        @NotEmpty
        String password,
        @NotNull
        String profile
) {
}
