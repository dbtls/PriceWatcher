package com.example.pricewatch.domain.price.dto;

import com.example.pricewatch.domain.price.entity.PriceHistory;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 가격 이력 항목 응답 DTO.
 */
public record PriceHistoryItemRes(
        LocalDate capturedAt,
        BigDecimal price
) {
    /**
     * 가격 이력 엔티티를 응답 DTO로 변환.
     */
    public static PriceHistoryItemRes from(PriceHistory entity) {
        return new PriceHistoryItemRes(entity.getCapturedAt(), entity.getPrice());
    }
}
