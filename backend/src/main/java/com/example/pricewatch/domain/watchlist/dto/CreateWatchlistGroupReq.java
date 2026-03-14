package com.example.pricewatch.domain.watchlist.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWatchlistGroupReq(
        @NotBlank
        String name
) {
}
