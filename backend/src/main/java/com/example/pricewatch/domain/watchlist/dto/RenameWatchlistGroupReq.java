package com.example.pricewatch.domain.watchlist.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameWatchlistGroupReq(
        @NotBlank
        String name
) {
}
