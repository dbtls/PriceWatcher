package com.example.pricewatch.domain.task.dto;

public record DbUpdateEvent(
        Long productId,
        TaskType taskType,
        String requestId
) {
}
