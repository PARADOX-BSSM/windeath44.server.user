package com.example.user.global.dto;

public record ResponseDto<T> (
        String message,
        T data
) {
}
