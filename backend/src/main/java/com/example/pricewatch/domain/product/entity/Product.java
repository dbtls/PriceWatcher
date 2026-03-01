package com.example.pricewatch.domain.product.entity;

import com.example.pricewatch.domain.category.entity.Category;
import com.example.pricewatch.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 상품 엔티티.
 */
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal price;

    private String mallName;

    @Column(unique = true)
    private String naverProductId;

    @Column(nullable = false, unique = true, length = 64)
    private String externalKey;

    @Column(nullable = false)
    private String url;

    private LocalDateTime lastSeenAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefreshStatus refreshStatus;

    @Column(nullable = false)
    private boolean needsRematch;

    @Column(nullable = false)
    private int failCount;

    /**
     * 가격 갱신 성공 상태 반영.
     */
    public void refresh(BigDecimal latestPrice, LocalDateTime seenAt) {
        this.price = latestPrice;
        this.lastSeenAt = seenAt;
        this.refreshStatus = RefreshStatus.UPDATED;
        this.needsRematch = false;
        this.failCount = 0;
    }

    /**
     * 가격 갱신 실패 상태 반영.
     */
    public void markRefreshFailed(RefreshStatus status) {
        this.refreshStatus = status;
        this.needsRematch = true;
        this.failCount++;
    }
}
