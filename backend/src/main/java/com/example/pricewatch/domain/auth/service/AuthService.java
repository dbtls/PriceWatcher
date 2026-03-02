package com.example.pricewatch.domain.auth.service;

import com.example.pricewatch.domain.auth.dto.request.LoginReq;
import com.example.pricewatch.domain.auth.dto.request.RegisterReq;
import com.example.pricewatch.domain.auth.dto.response.LoginRes;
import com.example.pricewatch.domain.auth.entity.RefreshToken;
import com.example.pricewatch.domain.auth.repository.RefreshTokenRepository;
import com.example.pricewatch.domain.user.entity.User;
import com.example.pricewatch.domain.user.entity.UserRole;
import com.example.pricewatch.domain.user.entity.UserStatus;
import com.example.pricewatch.domain.user.repository.UserRepository;
import com.example.pricewatch.global.exception.ApiException;
import com.example.pricewatch.global.exception.ErrorCode;
import com.example.pricewatch.global.security.JwtTokenProvider;
import com.example.pricewatch.global.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 인증 비즈니스 로직 서비스.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    @Value("${jwt.refresh-token-validity:1209600000}")
    private long refreshTokenValidityMs;

    /**
     * 로그인 결과 컨테이너.
     */
    public record LoginResult(
            LoginRes loginRes,
            String refreshToken
    ) {
    }

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
    @Transactional
    public LoginResult login(LoginReq req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ApiException(ErrorCode.AUTH_REQUIRED));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.AUTH_REQUIRED);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String rawRefreshToken = UUID.randomUUID().toString();

        // DB에는 원문 대신 해시 저장.
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(HashUtil.sha256(rawRefreshToken))
                .familyId(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenValidityMs)))
                .build();
        refreshTokenRepository.save(refreshToken);

        return new LoginResult(LoginRes.of(accessToken), rawRefreshToken);
    }
}

