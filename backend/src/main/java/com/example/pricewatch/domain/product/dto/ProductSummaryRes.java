package com.example.pricewatch.domain.product.dto;

import com.example.pricewatch.domain.product.entity.Product;

import java.math.BigDecimal;

public record ProductSummaryRes(
        Long productId,
        String brand,
        String title,
        BigDecimal price,
        String mallName,
        String naverProductId,
        String externalKey,
        String url,
        String categoryPath
) {
    public static ProductSummaryRes from(Product product) {
        String categoryPath = product.getCategory() == null ? null : product.getCategory().getPath();
        return new ProductSummaryRes(
                product.getId(),
                product.getBrand(),
                product.getTitle(),
                product.getPrice(),
                product.getMallName(),
                product.getNaverProductId(),
                product.getExternalKey(),
                product.getUrl(),
                categoryPath
        );
    }

    public static ProductSummaryRes external(
            String brand,
            String title,
            BigDecimal price,
            String mallName,
            String naverProductId,
            String externalKey,
            String url,
            String categoryPath
    ) {
        return new ProductSummaryRes(
                null,
                brand,
                title,
                price,
                mallName,
                naverProductId,
                externalKey,
                url,
                categoryPath
        );
    }
}
