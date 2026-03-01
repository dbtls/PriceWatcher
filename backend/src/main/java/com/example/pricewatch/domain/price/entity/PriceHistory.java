package com.example.pricewatch.domain.price.entity;

import com.example.pricewatch.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일자별 가격 스냅샷 엔티티.
 */
@Entity
@Table(name = "price_history", uniqueConstraints = @UniqueConstraint(name = "uk_price_history_product_date", columnNames = {"product_id", "captured_at"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal price;

    @Column(name = "captured_at", nullable = false)
    private LocalDate capturedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 가격 이력 정적 생성.
     */
    public static PriceHistory of(Product product, BigDecimal price, LocalDate capturedAt) {
        return PriceHistory.builder()
                .product(product)
                .price(price)
                .capturedAt(capturedAt)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
