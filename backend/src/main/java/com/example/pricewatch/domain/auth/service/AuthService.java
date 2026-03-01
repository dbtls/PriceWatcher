package com.example.pricewatch.domain.auth.service;

import com.example.pricewatch.domain.auth.dto.request.LoginReq;
import com.example.pricewatch.domain.auth.dto.request.RegisterReq;
import com.example.pricewatch.domain.auth.dto.response.LoginRes;
import com.example.pricewatch.domain.user.entity.User;
import com.example.pricewatch.domain.user.entity.UserRole;
import com.example.pricewatch.domain.user.entity.UserStatus;
import com.example.pricewatch.domain.user.repository.UserRepository;
import com.example.pricewatch.global.exception.ApiException;
import com.example.pricewatch.global.exception.ErrorCode;
import com.example.pricewatch.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 비즈니스 로직 서비스.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 신규 사용자를 등록.
     */
    @Transactional
    public void register(RegisterReq req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ApiException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 비밀번호는 해시 후 저장.
        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .nickname(req.nickname())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);
    }

    /**
     * 사용자 인증 후 Access Token을 발급.
     */
    @Transactional(readOnly = true)
    public LoginRes login(LoginReq req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_REQUIRED));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED);
        }

        // MVP에서는 Access Token만 서비스에서 생성.
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        return LoginRes.of(accessToken);
    }
}

