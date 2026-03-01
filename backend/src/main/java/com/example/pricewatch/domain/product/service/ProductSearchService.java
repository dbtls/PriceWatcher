package com.example.pricewatch.domain.product.service;

import com.example.pricewatch.domain.product.dto.ProductSearchRes;
import com.example.pricewatch.domain.product.dto.ProductSummaryRes;
import com.example.pricewatch.domain.product.repository.ProductRepository;
import com.example.pricewatch.global.util.HashUtil;
import com.example.pricewatch.global.util.RedisKeys;
import com.example.pricewatch.global.util.UserLockExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 상품 검색 서비스.
 */
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final UserLockExecutor userLockExecutor;

    /**
     * DB 우선 검색 후 외부 연동 준비 로직 수행.
     */
    @Transactional(readOnly = true)
    public ProductSearchRes search(String q) {
        List<ProductSummaryRes> dbTop5 = productRepository
                .findTop5ByBrandContainingIgnoreCaseOrTitleContainingIgnoreCase(q, q)
                .stream()
                .map(ProductSummaryRes::from)
                .toList();

        // 검색 single-flight 락 키 계산.
        String queryHash = HashUtil.sha256(q);
        // TODO 외부 API 호출 시 중복 실행 방지.
        userLockExecutor.withLock(RedisKeys.searchLock(queryHash), () -> null);

        return ProductSearchRes.of(dbTop5, List.of(), false);
    }
}
