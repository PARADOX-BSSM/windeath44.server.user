package com.example.user.domain.presentation.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest (
        @NotNull(message="userId is null")
        String userId,
        @NotNull(message="email is null")
        @Email(message="email is incorrect")
        String email,
        @NotEmpty(message="name is null")
        String name,
        @NotNull(message="password is null")
        @NotBlank
        String password
) {
}
