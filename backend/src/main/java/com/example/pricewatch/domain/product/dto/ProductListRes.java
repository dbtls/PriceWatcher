package com.example.pricewatch.domain.product.dto;

import java.util.List;

public record ProductListRes(
        List<ProductSummaryRes> items,
        int page,
        int size,
        long totalCount,
        boolean hasNext
) {
    public static ProductListRes of(List<ProductSummaryRes> items, int page, int size, long totalCount) {
        return new ProductListRes(
                items,
                page,
                size,
                totalCount,
                (long) (page + 1) * size < totalCount
        );
    }
}
