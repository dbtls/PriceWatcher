package com.example.pricewatch.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "watch_item",
        indexes = {
                @Index(name = "idx_watch_user_id", columnList = "user_id"),
                @Index(name = "idx_watch_product_id", columnList = "product_id"),
                @Index(name = "idx_watch_user_product", columnList = "user_id, product_id")
        },
        uniqueConstraints = {
                // 한 유저가 같은 상품을 중복 등록 못하게(MVP 기준)
                @UniqueConstraint(name = "uk_watch_user_product", columnNames = {"user_id", "product_id"})
        }
)
public class WatchItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // "여러 몰 통합 최저가" 기준이면 Product를 바라보는 게 정답
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // 등록 시점 기준가(= createPrice 역할). dropPercent 평가할 때 사용
    @Column(name = "base_price")
    private Integer basePrice;

    // 기준 하락값
    @Column(name = "threshold_price")
    private Integer thresholdPrice;

    // 기준 하락률 (nullable) - MVP는 정수 %로 (10 = 10%)
    @Column(name = "drop_percent")
    private Integer dropPercent;

    // 중복 알림 방지용(선택)
    @Column(name = "last_notified_price")
    private Integer lastNotifiedPrice;

    @Column(name = "last_notified_at")
    private LocalDateTime lastNotifiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public boolean shouldNotify(int currentLowestPrice) {
        // 1. 규칙 만족 여부
        if (!isRuleSatisfied(currentLowestPrice)) {
            return false;
        }

        // 2. 중복 알림 방지
        if (isDuplicateNotification(currentLowestPrice)) {
            return false;
        }

        return true;
    }

    /**
     * 알림 규칙(임계가 / 하락률) 만족 여부
     */
    private boolean isRuleSatisfied(int currentLowestPrice) {
        // 임계가 조건
        if (thresholdPrice != null && currentLowestPrice <= thresholdPrice) {
            return true;
        }

        // 하락률 조건
        if (dropPercent != null && basePrice != null && basePrice > 0) {
            int targetPrice = basePrice * (100 - dropPercent) / 100;
            return currentLowestPrice <= targetPrice;
        }

        return false;
    }

    /**
     * 같은 가격으로 연속 알림 방지
     */
    private boolean isDuplicateNotification(int currentLowestPrice) {
        return lastNotifiedPrice != null && lastNotifiedPrice == currentLowestPrice;
    }


}
