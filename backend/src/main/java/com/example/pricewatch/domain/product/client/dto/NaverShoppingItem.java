package com.example.pricewatch.domain.product.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverShoppingItem(
        String title,
        String link,
        String image,
        String lprice,
        String hprice,
        String mallName,
        String productId,
        String brand,
        String category1,
        String category2,
        String category3,
        String category4
) {
}

