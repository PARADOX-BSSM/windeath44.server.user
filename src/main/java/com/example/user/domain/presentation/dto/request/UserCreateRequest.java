package com.example.user.domain.presentation.dto.request;


import jakarta.validation.constraints.*;

public record UserCreateRequest (
        @NotNull(message="userId is null")
        @Size(min=6, max=16)
        String userId,
        @NotNull(message="email is null")
        @Email(message="email is incorrect")
        String email,
        @NotEmpty(message="name is null")
        String name,
        @NotNull(message="password is null")
        @NotBlank
        @Size(min=8, max=20)
        String password
) {
}
