package com.example.pricewatch.domain.user.dto;

import java.util.List;

public record MyPageSummaryRes(
        ProfileRes profile,
        long watchlistCount,
        long watchlistGroupCount,
        long unreadNotificationCount,
        long targetReachedCount,
        List<MyPageRecentDropRes> recentDrops
) {
}
