package com.example.pricewatch.domain.auth.controller;

import com.example.pricewatch.domain.auth.dto.request.LoginReq;
import com.example.pricewatch.domain.auth.dto.request.RegisterReq;
import com.example.pricewatch.domain.auth.dto.response.LoginRes;
import com.example.pricewatch.domain.auth.service.AuthService;
import com.example.pricewatch.global.dto.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", loginResult.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/auth/refresh")
                .maxAge(60L * 60L * 24L * 14L)
                .build();

        // Refresh Token은 HttpOnly 쿠키로 전달.
        return ResponseEntity.ok()
                .header("Set-Cookie", refreshCookie.toString())
                .body(ResponseDto.success("로그인 성공", loginResult.loginRes()));
    }

    /**
     * Refresh Token으로 Access Token을 재발급.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ResponseDto<LoginRes>> refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        return ResponseEntity.ok(ResponseDto.success("토큰 재발급 성공", LoginRes.of("TODO_ACCESS_TOKEN")));
    }

    /**
     * 현재 세션 로그아웃을 처리.
     */
    @PostMapping("/logout")
    public ResponseEntity<ResponseDto<Void>> logout() {
        return ResponseEntity.ok(ResponseDto.success("로그아웃 성공"));
    }
}

