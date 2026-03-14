package com.example.pricewatch.domain.user.dto;

import java.math.BigDecimal;

public record MyPageRecentDropRes(
        Long productId,
        String title,
        String imageUrl,
        BigDecimal currentPrice,
        BigDecimal previousPrice,
        BigDecimal dropAmount,
        int dropRatePercent
) {
}
