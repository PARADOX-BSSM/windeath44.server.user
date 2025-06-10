package com.example.user.global.mapper.dto;

public record ResponseDto<T> (
        String message,
        T data
) {
}
