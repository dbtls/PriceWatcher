package com.example.pricewatch.domain.email.dto;

public record EmailOutboxStatsRes(
        long pending,
        long sent,
        long failed
) {
}
