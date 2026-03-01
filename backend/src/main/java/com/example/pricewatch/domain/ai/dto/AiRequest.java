package com.example.pricewatch.domain.ai.dto;

/**
 * AI 요청 DTO.
 */
public record AiRequest(
        String intent,
        String message
) {
}
