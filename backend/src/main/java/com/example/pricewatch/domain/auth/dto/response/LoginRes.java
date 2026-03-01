package com.example.pricewatch.domain.auth.dto.response;

/**
 * 로그인 응답 DTO.
 */
public record LoginRes(
        String accessToken,
        String tokenType
) {
    /**
     * Access Token 기반 응답 생성.
     */
    public static LoginRes of(String accessToken) {
        return new LoginRes(accessToken, "Bearer");
    }
}
