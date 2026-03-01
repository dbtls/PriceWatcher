package com.example.pricewatch.domain.user.dto;

import com.example.pricewatch.domain.user.entity.User;

/**
 * 내 프로필 응답 DTO.
 */
public record ProfileRes(
        Long id,
        String email,
        String nickname,
        String role,
        String status
) {
    /**
     * 사용자 엔티티를 프로필 응답으로 변환.
     */
    public static ProfileRes from(User user) {
        return new ProfileRes(user.getId(), user.getEmail(), user.getNickname(), user.getRole().name(), user.getStatus().name());
    }
}
