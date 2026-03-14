package com.example.pricewatch.domain.watchlist.dto;

import java.util.List;

public record WatchlistGroupDetailRes(
        Long groupId,
        String name,
        int itemCount,
        List<WatchlistGroupItemRes> items
) {
}
