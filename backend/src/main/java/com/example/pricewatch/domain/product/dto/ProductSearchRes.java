package com.example.pricewatch.domain.product.dto;

import java.util.List;

public record ProductSearchRes(
        List<ProductSummaryRes> internalResults,
        List<ProductSummaryRes> externalResults,
        boolean degraded,
        int page,
        int size,
        long totalCount,
        boolean hasNext
) {
    public static ProductSearchRes of(
            List<ProductSummaryRes> internalResults,
            List<ProductSummaryRes> externalResults,
            boolean degraded,
            int page,
            int size,
            long totalCount
    ) {
        return new ProductSearchRes(
                internalResults,
                externalResults,
                degraded,
                page,
                size,
                totalCount,
                (long) (page + 1) * size < totalCount
        );
    }
}
