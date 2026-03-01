package com.example.pricewatch.domain.watchlist.dto;

import java.math.BigDecimal;

/**
 * 워치리스트 목표가 변경 요청 DTO.
 */
public record UpdateTargetPriceReq(
        BigDecimal targetPrice
) {
}
