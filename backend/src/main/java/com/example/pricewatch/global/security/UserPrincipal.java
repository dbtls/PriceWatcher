package com.example.pricewatch.global.security;

import lombok.Builder;
import lombok.Getter;

/**
 * 인증 컨텍스트에서 사용하는 사용자 정보 객체.
 */
@Getter
@Builder
public class UserPrincipal {
    private Long userId;
    private String email;
    private String role;
}
