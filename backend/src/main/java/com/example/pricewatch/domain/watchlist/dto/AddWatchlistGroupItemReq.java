package com.example.pricewatch.domain.watchlist.dto;

import jakarta.validation.constraints.NotNull;

public record AddWatchlistGroupItemReq(
        @NotNull
        Long productId
) {
}
