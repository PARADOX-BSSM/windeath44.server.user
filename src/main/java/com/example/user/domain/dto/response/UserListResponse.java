package com.example.user.domain.dto.response;

import java.util.List;

public record UserListResponse(
        List<UserDetailResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
