package com.example.pricewatch.domain.notification.entity;

import com.example.pricewatch.domain.user.entity.User;
import com.example.pricewatch.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 인앱 알림 엔티티.
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private boolean isRead;

    private Long productId;

    /**
     * 알림 정적 생성.
     */
    public static Notification of(User user, NotificationType type, String message, Long productId) {
        return Notification.builder().user(user).type(type).message(message).isRead(false).productId(productId).build();
    }

    /**
     * 알림 읽음 상태 반영.
     */
    public void read() {
        this.isRead = true;
    }
}
