package com.example.pricewatch.domain.notification.service;

import com.example.pricewatch.domain.notification.dto.NotificationRes;
import com.example.pricewatch.domain.notification.entity.Notification;
import com.example.pricewatch.domain.notification.repository.NotificationRepository;
import com.example.pricewatch.global.exception.ApiException;
import com.example.pricewatch.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 알림 서비스.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * 사용자 알림 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<NotificationRes> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationRes::from)
                .toList();
    }

    /**
     * 알림 읽음 처리.
     */
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }
        notification.read();
    }
}
