package com.example.pricewatch.domain.product.entity;

/**
 * 상품 가격 갱신 상태.
 */
public enum RefreshStatus {
    READY,
    UPDATED,
    NOT_FOUND,
    MISMATCH,
    FAILED,
    BLOCKED
}
