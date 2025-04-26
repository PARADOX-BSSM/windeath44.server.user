package com.example.user.domain.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record UserNameUpdateRequest(
        @NotNull(message="name is null")
        String name
) {
}
