package com.example.pricewatch.domain.product.service;

import com.example.pricewatch.domain.category.entity.Category;
import com.example.pricewatch.domain.category.service.CategoryService;
import com.example.pricewatch.domain.product.client.NaverShoppingClient;
import com.example.pricewatch.domain.product.client.dto.NaverShoppingItem;
import com.example.pricewatch.domain.product.client.dto.NaverShoppingSearchResponse;
import com.example.pricewatch.domain.product.dto.ProductSearchRes;
import com.example.pricewatch.domain.product.dto.ProductSummaryRes;
import com.example.pricewatch.domain.product.repository.ProductRepository;
import com.example.pricewatch.global.exception.ApiException;
import com.example.pricewatch.global.exception.ErrorCode;
import com.example.pricewatch.global.util.HashUtil;
import com.example.pricewatch.global.util.LinkNormalizer;
import com.example.pricewatch.global.util.RedisKeys;
import com.example.pricewatch.global.util.UserLockExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final UserLockExecutor userLockExecutor;
    private final NaverShoppingClient naverShoppingClient;
    private final CategoryService categoryService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${naver.shopping.daily-quota:20000}")
    private long naverDailyQuota;
    @Value("${naver.shopping.search-cache-ttl-seconds:1800}")
    private long searchCacheTtlSeconds;
    @Value("${naver.shopping.display:100}")
    private int naverDisplay;

    @Transactional
    public ProductSearchRes search(String q) {
        if (q == null || q.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String keyword = q.trim();
        List<ProductSummaryRes> dbTop5 = productRepository
                .findTop5ByBrandContainingIgnoreCaseOrTitleContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(ProductSummaryRes::from)
                .toList();

        String queryHash = HashUtil.sha256(keyword);
        SearchCachePayload cached = getSearchCache(queryHash);
        if (cached != null) {
            return ProductSearchRes.of(dbTop5, cached.externalResults(), cached.degraded());
        }

        SearchCachePayload payload = userLockExecutor.withLock(RedisKeys.searchLock(queryHash), () -> {
            SearchCachePayload lockCached = getSearchCache(queryHash);
            if (lockCached != null) {
                return lockCached;
            }

            if (!naverShoppingClient.isConfigured() || !tryConsumeNaverQuota()) {
                SearchCachePayload degradedPayload = new SearchCachePayload(List.of(), true);
                putSearchCache(queryHash, degradedPayload);
                return degradedPayload;
            }

            try {
                NaverShoppingSearchResponse response = naverShoppingClient.search(keyword, naverDisplay);
                List<ProductSummaryRes> mapped = mapExternalItems(response.items());
                SearchCachePayload successPayload = new SearchCachePayload(mapped, false);
                putSearchCache(queryHash, successPayload);
                return successPayload;
            } catch (Exception e) {
                SearchCachePayload degradedPayload = new SearchCachePayload(List.of(), true);
                putSearchCache(queryHash, degradedPayload);
                return degradedPayload;
            }
        });

        return ProductSearchRes.of(dbTop5, payload.externalResults(), payload.degraded());
    }

    private List<ProductSummaryRes> mapExternalItems(List<NaverShoppingItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<ProductSummaryRes> results = new ArrayList<>();
        Set<String> dedupe = new HashSet<>();

        for (NaverShoppingItem item : items) {
            String naverProductId = safe(item.productId());
            String normalizedLink = LinkNormalizer.normalize(safe(item.link()));
            String externalKey = HashUtil.sha256(normalizedLink);
            String dedupeKey = !naverProductId.isBlank() ? "N:" + naverProductId : "E:" + externalKey;
            if (!dedupe.add(dedupeKey)) {
                continue;
            }

            Category leaf = categoryService.resolveLeaf(List.of(
                    safe(item.category1()),
                    safe(item.category2()),
                    safe(item.category3()),
                    safe(item.category4())
            ));
            String categoryPath = leaf == null ? null : leaf.getPath();

            results.add(ProductSummaryRes.external(
                    safe(item.brand()),
                    sanitizeTitle(item.title()),
                    parsePrice(item.lprice()),
                    safe(item.mallName()),
                    naverProductId.isBlank() ? null : naverProductId,
                    externalKey,
                    normalizedLink,
                    categoryPath
            ));
        }

        return results;
    }

    private boolean tryConsumeNaverQuota() {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = RedisKeys.naverDailyQuota(today);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            return false;
        }
        if (count == 1L) {
            Duration ttl = Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atTime(LocalTime.MIN));
            redisTemplate.expire(key, ttl);
        }
        return count <= naverDailyQuota;
    }

    private SearchCachePayload getSearchCache(String queryHash) {
        String cached = redisTemplate.opsForValue().get(RedisKeys.searchCache(queryHash));
        if (cached == null || cached.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(cached, SearchCachePayload.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private void putSearchCache(String queryHash, SearchCachePayload payload) {
        try {
            String serialized = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(
                    RedisKeys.searchCache(queryHash),
                    serialized,
                    Duration.ofSeconds(searchCacheTtlSeconds)
            );
        } catch (JsonProcessingException ignored) {
        }
    }

    private String sanitizeTitle(String rawTitle) {
        String base = safe(rawTitle);
        return base.replaceAll("<[^>]*>", "").trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private BigDecimal parsePrice(String rawPrice) {
        if (rawPrice == null || rawPrice.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(rawPrice.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private record SearchCachePayload(
            List<ProductSummaryRes> externalResults,
            boolean degraded
    ) {
    }
}
