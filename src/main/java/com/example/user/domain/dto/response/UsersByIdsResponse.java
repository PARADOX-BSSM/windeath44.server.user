package com.example.user.domain.dto.response;

import java.util.List;

public record UsersByIdsResponse(
        List<UserDetailResponse> users,
        List<String> notFoundUserIds
) {
}
