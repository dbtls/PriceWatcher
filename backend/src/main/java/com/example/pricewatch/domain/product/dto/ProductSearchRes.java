package com.example.pricewatch.domain.product.dto;

import java.util.List;

public record ProductSearchRes(
        List<ProductSummaryRes> dbTop5,
        List<ProductSummaryRes> externalResults,
        boolean degraded
) {
    public static ProductSearchRes of(List<ProductSummaryRes> dbTop5, List<ProductSummaryRes> externalResults, boolean degraded) {
        return new ProductSearchRes(dbTop5, externalResults, degraded);
    }
}
