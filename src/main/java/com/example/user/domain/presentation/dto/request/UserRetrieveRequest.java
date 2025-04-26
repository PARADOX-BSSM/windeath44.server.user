package com.example.user.domain.presentation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRetrieveRequest (
        @Email
        @NotNull(message="email is null")
        String email,
        @NotNull(message="password is null")
        @Size(min = 8, max=20)
        String password
) {
}
