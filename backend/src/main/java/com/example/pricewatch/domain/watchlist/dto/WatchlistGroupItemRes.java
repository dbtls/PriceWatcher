package com.example.pricewatch.domain.watchlist.dto;

import com.example.pricewatch.domain.price.dto.PriceHistoryItemRes;

import java.math.BigDecimal;
import java.util.List;

public record WatchlistGroupItemRes(
        Long productId,
        String brand,
        String title,
        String mallName,
        String imageUrl,
        BigDecimal currentPrice,
        BigDecimal targetPrice,
        BigDecimal lowestPrice,
        String url,
        List<PriceHistoryItemRes> priceHistory
) {
}
