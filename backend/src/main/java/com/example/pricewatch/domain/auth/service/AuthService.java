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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    @Value("${jwt.refresh-token-validity:1209600000}")
    private long refreshTokenValidityMs;

    public record LoginResult(
            LoginRes loginRes,
            String refreshToken
    ) {
    }

    @Transactional
    public void register(RegisterReq req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ApiException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .nickname(req.nickname())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);
    }

    @Transactional
    public LoginResult login(LoginReq req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_LOGIN));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_LOGIN);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String rawRefreshToken = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(HashUtil.sha256(rawRefreshToken))
                .familyId(UUID.randomUUID().toString())
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenValidityMs)))
                .build();
        refreshTokenRepository.save(refreshToken);

        return new LoginResult(LoginRes.of(accessToken), rawRefreshToken);
    }

    @Transactional
    public LoginResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_TOKEN);
        }

        String tokenHash = HashUtil.sha256(rawRefreshToken);
        RefreshToken currentToken = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TOKEN));

        LocalDateTime now = LocalDateTime.now();
        if (currentToken.isExpired(now)) {
            currentToken.revokeNow();
            throw new ApiException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        currentToken.revokeNow();

        String nextRawRefreshToken = UUID.randomUUID().toString();
        RefreshToken rotatedToken = RefreshToken.builder()
                .user(currentToken.getUser())
                .tokenHash(HashUtil.sha256(nextRawRefreshToken))
                .familyId(currentToken.getFamilyId())
                .expiresAt(now.plus(Duration.ofMillis(refreshTokenValidityMs)))
                .build();
        refreshTokenRepository.save(rotatedToken);

        User user = currentToken.getUser();
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());

        return new LoginResult(LoginRes.of(accessToken), nextRawRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        String tokenHash = HashUtil.sha256(rawRefreshToken);
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash)
                .ifPresent(RefreshToken::revokeNow);
    }
}
