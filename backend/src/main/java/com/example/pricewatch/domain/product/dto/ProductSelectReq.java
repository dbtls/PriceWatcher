package com.example.pricewatch.domain.product.dto;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductSelectReq(
        Long productId,
        String brand,
        String title,
        @PositiveOrZero BigDecimal price,
        String mallName,
        String naverProductId,
        String externalKey,
        String url,
        String categoryPath
) {
}
