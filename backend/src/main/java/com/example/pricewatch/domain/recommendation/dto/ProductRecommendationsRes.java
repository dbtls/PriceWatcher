package com.example.pricewatch.domain.recommendation.dto;

import com.example.pricewatch.domain.product.dto.ProductSummaryRes;

import java.util.List;

public record ProductRecommendationsRes(
        List<ProductSummaryRes> brandSimilarProducts,
        List<ProductSummaryRes> similarProducts
) {
}
