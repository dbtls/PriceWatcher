package com.example.pricewatch.domain.user.service;

import com.example.pricewatch.domain.user.dto.ProfileRes;
import com.example.pricewatch.domain.user.entity.User;
import com.example.pricewatch.domain.user.repository.UserRepository;
import com.example.pricewatch.global.exception.ApiException;
import com.example.pricewatch.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 도메인 서비스.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * 내 프로필 조회.
     */
    @Transactional(readOnly = true)
    public ProfileRes getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return ProfileRes.from(user);
    }
}
