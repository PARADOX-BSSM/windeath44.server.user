package com.example.user.domain.presentation.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public record UserUpdateRequest (
        @NotEmpty
        String name,
        @NotEmpty
        String password,
        @Pattern(regexp = "[a-zA-Z0-9._-]+\\.png$")
        String profile
) {
}
