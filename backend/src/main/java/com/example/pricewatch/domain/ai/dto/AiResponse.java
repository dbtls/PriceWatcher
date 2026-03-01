package com.example.pricewatch.domain.ai.dto;

import java.util.List;

/**
 * AI 응답 DTO.
 */
public record AiResponse(
        String intent,
        List<Long> productIds,
        List<List<Long>> outfits,
        String explanation
) {
    /**
     * 기본 빈 응답 생성.
     */
    public static AiResponse empty(String intent) {
        return new AiResponse(intent, List.of(), List.of(), "TODO: AI result");
    }
}
