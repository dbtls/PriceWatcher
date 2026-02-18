package com.example.pricewatch.catalog.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(
        name = "offer",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_offer_mall_url", columnNames = {"mall", "url"})
        },
        indexes = {
                @Index(name = "idx_offer_product_id", columnList = "product_id"),
                @Index(name = "idx_offer_mall", columnList = "mall")
        }
)
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MallType mall;

    @Column(nullable = false, length = 500)
    private String url;
}




