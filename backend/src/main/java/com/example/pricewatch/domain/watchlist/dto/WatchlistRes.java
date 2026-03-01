package com.example.pricewatch.domain.watchlist.dto;

import com.example.pricewatch.domain.watchlist.entity.Watchlist;

import java.math.BigDecimal;

/**
 * 워치리스트 응답 DTO.
 */
public record WatchlistRes(
        Long watchlistId,
        Long productId,
        String title,
        BigDecimal targetPrice
) {
    /**
     * 워치리스트 엔티티를 응답 DTO로 변환.
     */
    public static WatchlistRes from(Watchlist watchlist) {
        return new WatchlistRes(
                watchlist.getId(),
                watchlist.getProduct().getId(),
                watchlist.getProduct().getTitle(),
                watchlist.getTargetPrice()
        );
    }
}
