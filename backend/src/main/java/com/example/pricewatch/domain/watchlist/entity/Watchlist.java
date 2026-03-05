package com.example.pricewatch.domain.watchlist.entity;

import com.example.pricewatch.domain.product.entity.Product;
import com.example.pricewatch.domain.user.entity.User;
import com.example.pricewatch.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "watchlists", uniqueConstraints = @UniqueConstraint(name = "uk_watchlist_user_product", columnNames = {"user_id", "product_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Watchlist extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(precision = 15, scale = 0)
    private BigDecimal targetPrice;

    public void updateTargetPrice(BigDecimal targetPrice) {
        this.targetPrice = targetPrice;
    }
}
