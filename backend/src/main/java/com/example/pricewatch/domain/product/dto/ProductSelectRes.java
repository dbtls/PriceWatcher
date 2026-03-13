package com.example.pricewatch.domain.product.dto;

public record ProductSelectRes(
        ProductSummaryRes product,
        boolean created
) {
    public static ProductSelectRes of(ProductSummaryRes product, boolean created) {
        return new ProductSelectRes(product, created);
    }
}
