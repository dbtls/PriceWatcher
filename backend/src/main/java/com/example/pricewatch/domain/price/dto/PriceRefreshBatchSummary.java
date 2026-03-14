package com.example.pricewatch.domain.price.dto;

import java.io.Serializable;

public record PriceRefreshBatchSummary(
        int firstPassGroups,
        int firstPassUpdated,
        int firstPassApiCalls,
        int secondPassCandidates,
        int secondPassUpdated,
        int secondPassApiCalls,
        int totalApiCalls,
        int failed,
        int targetPriceNotifications,
        int newLowestNotifications,
        int priceDropNotifications,
    boolean quotaExhausted
) implements Serializable {
    public static PriceRefreshBatchSummary empty() {
        return new PriceRefreshBatchSummary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false);
    }

    public PriceRefreshBatchSummary merge(PriceRefreshBatchSummary other) {
        return new PriceRefreshBatchSummary(
                this.firstPassGroups + other.firstPassGroups,
                this.firstPassUpdated + other.firstPassUpdated,
                this.firstPassApiCalls + other.firstPassApiCalls,
                this.secondPassCandidates + other.secondPassCandidates,
                this.secondPassUpdated + other.secondPassUpdated,
                this.secondPassApiCalls + other.secondPassApiCalls,
                this.totalApiCalls + other.totalApiCalls,
                this.failed + other.failed,
                this.targetPriceNotifications + other.targetPriceNotifications,
                this.newLowestNotifications + other.newLowestNotifications,
                this.priceDropNotifications + other.priceDropNotifications,
                this.quotaExhausted || other.quotaExhausted
        );
    }
}
