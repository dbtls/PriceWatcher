package com.example.pricewatch.domain.watchlist.dto;

import java.util.List;

public record WatchlistGroupRes(
        Long groupId,
        String name,
        int itemCount,
        List<WatchlistGroupPreviewItemRes> previewItems
) {
}
