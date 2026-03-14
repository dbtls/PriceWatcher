package com.example.pricewatch.domain.user.controller;

import com.example.pricewatch.domain.user.dto.ProfileRes;
import com.example.pricewatch.domain.user.dto.MyPageSummaryRes;
import com.example.pricewatch.domain.user.service.UserService;
import com.example.pricewatch.global.dto.ResponseDto;
import com.example.pricewatch.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 API 컨트롤러.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    /**
     * 로그인 사용자 프로필 조회.
     */
    @GetMapping("/me")
    public ResponseEntity<ResponseDto<ProfileRes>> myProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ResponseDto.success("프로필 조회 성공", userService.getMyProfile(principal.getUserId())));
    }

    @GetMapping("/me/summary")
    public ResponseEntity<ResponseDto<MyPageSummaryRes>> myPageSummary(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ResponseDto.success("마이페이지 요약 조회 성공", userService.getMyPageSummary(principal.getUserId())));
    }
}
