package com.example.pricewatch.domain.auth.controller;

import com.example.pricewatch.domain.auth.dto.request.LoginReq;
import com.example.pricewatch.domain.auth.dto.request.RegisterReq;
import com.example.pricewatch.domain.auth.dto.response.LoginRes;
import com.example.pricewatch.domain.auth.service.AuthService;
import com.example.pricewatch.global.dto.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API 컨트롤러.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    @Value("${app.auth.refresh-cookie-name:refreshToken}")
    private String refreshCookieName;
    @Value("${app.auth.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;
    @Value("${jwt.refresh-token-validity:1209600000}")
    private long refreshTokenValidityMs;

    /**
     * 회원가입을 처리.
     */
    @PostMapping("/register")
    public ResponseEntity<ResponseDto<Void>> register(@Valid @RequestBody RegisterReq req) {
        authService.register(req);
        return ResponseEntity.ok(ResponseDto.success("회원가입 성공"));
    }

    /**
     * 로그인 후 Access Token과 Refresh Cookie를 발급.
     */
    @PostMapping("/login")
    public ResponseEntity<ResponseDto<LoginRes>> login(@Valid @RequestBody LoginReq req) {
        AuthService.LoginResult loginResult = authService.login(req);

        return ResponseEntity.ok()
                .header("Set-Cookie", createRefreshCookie(loginResult.refreshToken()).toString())
                .body(ResponseDto.success("로그인 성공", loginResult.loginRes()));
    }

    /**
     * Refresh Token으로 Access Token을 재발급.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ResponseDto<LoginRes>> refresh(
            @CookieValue(value = "${app.auth.refresh-cookie-name:refreshToken}", required = false) String refreshToken
    ) {
        AuthService.LoginResult loginResult = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header("Set-Cookie", createRefreshCookie(loginResult.refreshToken()).toString())
                .body(ResponseDto.success("토큰 재발급 성공", loginResult.loginRes()));
    }

    /**
     * 현재 세션 로그아웃을 처리.
     */
    @PostMapping("/logout")
    public ResponseEntity<ResponseDto<Void>> logout(
            @CookieValue(value = "${app.auth.refresh-cookie-name:refreshToken}", required = false) String refreshToken
    ) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header("Set-Cookie", clearRefreshCookie().toString())
                .body(ResponseDto.success("로그아웃 성공"));
    }

    private ResponseCookie createRefreshCookie(String refreshToken) {
        return ResponseCookie.from(refreshCookieName, refreshToken)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/auth")
                .maxAge(refreshTokenValidityMs / 1000)
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Lax")
                .path("/auth")
                .maxAge(0)
                .build();
    }
}

