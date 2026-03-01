package com.example.pricewatch.domain.recommendation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 연관 상품 추천 서비스.
 */
@Service
@RequiredArgsConstructor
public class RelatedProductService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 연관 상품 상위 N개 조회.
     */
    public List<Long> getRelated(Long productId, int topN) {
        return List.of();
    }

    /**
     * 사용자 상품 조회 이벤트 적재.
     */
    public void trackView(Long userId, Long productId) {
    }
}
