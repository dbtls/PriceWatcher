package com.example.pricewatch.pricing.domain.model;

import com.example.pricewatch.catalog.domain.model.Offer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "price_snapshot",
        indexes = {
                @Index(name = "idx_snapshot_offer_checked_at", columnList = "offer_id, checked_at"),
                @Index(name = "idx_snapshot_checked_at", columnList = "checked_at")
        }
)
public class PriceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @Column(nullable = false)
    private int price;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;
}




