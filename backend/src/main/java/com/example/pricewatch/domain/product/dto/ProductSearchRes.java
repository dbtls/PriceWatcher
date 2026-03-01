package com.example.pricewatch.domain.product.dto;

import java.util.List;

/**
 * 상품 검색 응답 DTO.
 */
public record ProductSearchRes(
        List<ProductSummaryRes> dbTop5,
        List<ProductSummaryRes> externalResults,
        boolean degraded
) {
    /**
     * 검색 응답 DTO 생성.
     */
    public static ProductSearchRes of(List<ProductSummaryRes> dbTop5, List<ProductSummaryRes> externalResults, boolean degraded) {
        return new ProductSearchRes(dbTop5, externalResults, degraded);
    }
}
