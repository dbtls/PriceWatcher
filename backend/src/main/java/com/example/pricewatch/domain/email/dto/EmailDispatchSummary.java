package com.example.pricewatch.domain.email.dto;

public record EmailDispatchSummary(
        int processed,
        int sent,
        int failed,
        int retried
) {
}
