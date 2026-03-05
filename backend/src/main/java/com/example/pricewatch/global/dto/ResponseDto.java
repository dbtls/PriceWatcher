package com.example.pricewatch.global.dto;

import java.time.LocalDateTime;

public record ResponseDto<T>(
        boolean success,
        String code,
        String message,
        T data,
        LocalDateTime timestamp
) {
    public static <T> ResponseDto<T> success(String message, T data) {
        return new ResponseDto<>(true, "OK", message, data, LocalDateTime.now());
    }

    public static <T> ResponseDto<T> success(String message) {
        return new ResponseDto<>(true, "OK", message, null, LocalDateTime.now());
    }

    public static <T> ResponseDto<T> error(String code, String message) {
        return new ResponseDto<>(false, code, message, null, LocalDateTime.now());
    }
}

