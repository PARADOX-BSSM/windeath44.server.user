package com.example.user.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record  UserProfileUpdateRequest (
        @NotNull(message="profile is null")
        MultipartFile profile
) {
}