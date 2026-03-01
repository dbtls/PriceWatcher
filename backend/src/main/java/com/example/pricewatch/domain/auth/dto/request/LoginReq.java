package com.example.pricewatch.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 DTO.
 */
public record LoginReq(
        @Email String email,
        @NotBlank String password
) {
}
