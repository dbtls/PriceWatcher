package com.example.pricewatch.domain.notification.controller;

import com.example.pricewatch.domain.notification.dto.NotificationRes;
import com.example.pricewatch.domain.notification.service.NotificationService;
import com.example.pricewatch.global.dto.ResponseDto;
import com.example.pricewatch.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 알림 API 컨트롤러.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 내 알림 목록 조회.
     */
    @GetMapping
    public ResponseEntity<ResponseDto<List<NotificationRes>>> getNotifications(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ResponseDto.success("알림 조회 성공", notificationService.getNotifications(principal.getUserId())));
    }

    /**
     * 알림 읽음 처리.
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ResponseDto<Void>> read(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        notificationService.markAsRead(principal.getUserId(), id);
        return ResponseEntity.ok(ResponseDto.success("알림 읽음 처리 성공"));
    }
}
