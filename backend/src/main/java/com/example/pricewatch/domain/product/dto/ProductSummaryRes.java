package com.example.pricewatch.domain.product.dto;

import com.example.pricewatch.domain.product.entity.Product;

import java.math.BigDecimal;

/**
 * 상품 요약 응답 DTO.
 */
public record ProductSummaryRes(
        Long productId,
        String brand,
        String title,
        BigDecimal price,
        String mallName
) {
    /**
     * 상품 엔티티를 요약 응답으로 변환.
     */
    public static ProductSummaryRes from(Product product) {
        return new ProductSummaryRes(product.getId(), product.getBrand(), product.getTitle(), product.getPrice(), product.getMallName());
    }
}
