package com.example.pricewatch.domain.category.dto;

import java.util.List;

public record CategoryNodeRes(
        Long id,
        String name,
        int depth,
        String path,
        List<CategoryNodeRes> children
) {
}
