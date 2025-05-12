package com.example.user.domain.presentation.dto.response;

public record ResponseDto<T> (
        String message,
        T data
) {

}
