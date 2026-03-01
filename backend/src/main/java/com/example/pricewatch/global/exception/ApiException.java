package com.example.pricewatch.global.exception;

import lombok.Getter;

/**
 * 비즈니스 로직에서 사용하는 커스텀 런타임 예외.
 */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    /**
     * 에러 코드를 기반으로 예외를 생성.
     */
    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

