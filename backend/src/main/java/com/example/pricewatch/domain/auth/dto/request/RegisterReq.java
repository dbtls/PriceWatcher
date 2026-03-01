package com.example.pricewatch.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 회원가입 요청 DTO.
 */
public record RegisterReq(
        @Email String email,
        @NotBlank String password,
        @NotBlank String nickname
) {
}
