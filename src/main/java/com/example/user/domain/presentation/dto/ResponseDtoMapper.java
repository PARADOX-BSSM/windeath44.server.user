package com.example.user.domain.presentation.dto;

import com.example.user.domain.presentation.dto.response.ResponseDto;
import org.springframework.stereotype.Component;

@Component
public class ResponseDtoMapper {

  public <T> ResponseDto<T> toResponseDto(String message, T data) {
    return new ResponseDto<>(message, data);
  }

}
