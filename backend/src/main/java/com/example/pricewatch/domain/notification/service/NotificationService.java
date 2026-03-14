package com.example.pricewatch.domain.notification.service;

import com.example.pricewatch.domain.email.service.EmailOutboxService;
import com.example.pricewatch.domain.notification.dto.NotificationRes;
import com.example.pricewatch.domain.notification.entity.Notification;
import com.example.pricewatch.domain.notification.entity.NotificationType;
import com.example.pricewatch.domain.notification.repository.NotificationRepository;
import com.example.pricewatch.domain.user.entity.User;
import com.example.pricewatch.global.exception.ApiException;
import com.example.pricewatch.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailOutboxService emailOutboxService;

    public List<NotificationRes> getNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationRes::from)
                .toList();
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }
        notification.read();
    }

    @Transactional
    public Notification create(User user, NotificationType type, String message, Long productId) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .message(message)
                .isRead(false)
                .productId(productId)
                .build();
        notificationRepository.save(notification);

        emailOutboxService.enqueue(
                user.getEmail(),
                "[PriceWatcher] " + type.name(),
                message
        );
        return notification;
    }
}
