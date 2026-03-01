package com.example.pricewatch.global.dto;

import java.time.LocalDateTime;

/**
 * API 응답 표준 포맷 DTO.
 */
public record ResponseDto<T>(
        boolean success,
        String code,
        String message,
        T data,
        LocalDateTime timestamp
) {
    /**
     * 성공 응답(데이터 포함)을 생성.
     */
    public static <T> ResponseDto<T> success(String message, T data) {
        return new ResponseDto<>(true, "OK", message, data, LocalDateTime.now());
    }

    /**
     * 성공 응답(데이터 없음)을 생성.
     */
    public static <T> ResponseDto<T> success(String message) {
        return new ResponseDto<>(true, "OK", message, null, LocalDateTime.now());
    }

    /**
     * 실패 응답을 생성.
     */
    public static <T> ResponseDto<T> error(String code, String message) {
        return new ResponseDto<>(false, code, message, null, LocalDateTime.now());
    }
}

