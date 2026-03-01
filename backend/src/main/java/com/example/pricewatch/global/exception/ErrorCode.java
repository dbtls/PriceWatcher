package com.example.pricewatch.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 전역 예외 코드 목록.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON-002", "요청 값이 올바르지 않습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "AUTH-001", "접근 권한이 없습니다."),
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH-002", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-003", "유효하지 않은 토큰입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "USER-002", "이미 사용중인 이메일입니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT-001", "상품을 찾을 수 없습니다."),
    WATCHLIST_CONFLICT(HttpStatus.CONFLICT, "WATCHLIST-001", "이미 워치리스트에 등록된 상품입니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTI-001", "알림을 찾을 수 없습니다."),
    LOCK_ACQUISITION_FAILED(HttpStatus.TOO_MANY_REQUESTS, "LOCK-001", "락 획득에 실패했습니다."),
    EXTERNAL_API_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "NAVER-001", "외부 API 일일 쿼터를 초과했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
