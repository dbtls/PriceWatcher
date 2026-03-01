package com.example.pricewatch.domain.notification.dto;

import com.example.pricewatch.domain.notification.entity.Notification;

/**
 * 알림 응답 DTO.
 */
public record NotificationRes(
        Long id,
        String type,
        String message,
        boolean isRead,
        Long productId
) {
    /**
     * 알림 엔티티를 응답 DTO로 변환.
     */
    public static NotificationRes from(Notification notification) {
        return new NotificationRes(notification.getId(), notification.getType().name(), notification.getMessage(), notification.isRead(), notification.getProductId());
    }
}
