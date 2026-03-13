package com.example.pricewatch.domain.product.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverShoppingSearchResponse(
        int total,
        int start,
        int display,
        List<NaverShoppingItem> items
) {
}

