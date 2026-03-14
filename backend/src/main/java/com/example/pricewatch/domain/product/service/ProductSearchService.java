package com.example.pricewatch.domain.product.service;

import com.example.pricewatch.domain.category.entity.Category;
import com.example.pricewatch.domain.category.service.CategoryService;
import com.example.pricewatch.domain.product.client.NaverShoppingClient;
import com.example.pricewatch.domain.product.client.dto.NaverShoppingItem;
import com.example.pricewatch.domain.product.client.dto.NaverShoppingSearchResponse;
import com.example.pricewatch.domain.product.dto.ProductSearchRes;
import com.example.pricewatch.domain.product.dto.ProductSummaryRes;
import com.example.pricewatch.domain.product.entity.Product;
import com.example.pricewatch.domain.product.repository.ProductRepository;
import com.example.pricewatch.domain.task.service.ProductSearchIndexService;
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
import org.springframework.data.jpa.domain.Specification;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private static final Set<String> ALLOWED_MALLS = Set.of(
            "무신사",
            "29CM",
            "하이츠스토어",
            "EQL"
    );
    private static final int MIN_DB_RESULT_COUNT = 5;
    private static final Pattern NORMALIZE_PATTERN = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHangul}]+");

    private final ProductRepository productRepository;
    private final UserLockExecutor userLockExecutor;
    private final NaverShoppingClient naverShoppingClient;
    private final CategoryService categoryService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ProductSearchIndexService productSearchIndexService;

    @Value("${naver.shopping.daily-quota:20000}")
    private long naverDailyQuota;
    @Value("${naver.shopping.search-cache-ttl-seconds:1800}")
    private long searchCacheTtlSeconds;
    @Value("${naver.shopping.display:100}")
    private int naverDisplay;
    @Value("${app.search.db-score-threshold:120}")
    private int dbScoreThreshold;

    @Transactional
    public ProductSearchRes searchDb(String q, int page, int size) {
        if (q == null || q.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (page < 0 || size <= 0) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }

        List<ProductSummaryRes> esResults = productSearchIndexService.search(q, page, size);
        if (esResults != null) {
            return ProductSearchRes.of(esResults, List.of(), false, page, size, esResults.size());
        }

        String keyword = q.trim();
        List<String> tokens = tokenize(keyword);
        if (tokens.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Specification<Product> specification = buildDbSearchSpecification(tokens);
        List<ScoredProduct> eligible = productRepository.findAll(specification)
                .stream()
                .map(product -> new ScoredProduct(product, scoreProduct(product, keyword, tokens)))
                .filter(scoredProduct -> scoredProduct.score() > 0)
                .sorted((left, right) -> {
                    int byScore = Integer.compare(right.score(), left.score());
                    if (byScore != 0) {
                        return byScore;
                    }
                    return Long.compare(left.product().getId(), right.product().getId());
                })
                .toList();

        int guaranteedCount = Math.min(MIN_DB_RESULT_COUNT, eligible.size());
        List<ScoredProduct> visible = eligible.stream()
                .filter(scored -> scored.score() >= dbScoreThreshold)
                .toList();
        if (visible.size() < guaranteedCount) {
            visible = eligible.subList(0, guaranteedCount);
        }

        int totalCount = visible.size();
        int fromIndex = Math.min(page * size, totalCount);
        int toIndex = Math.min(fromIndex + size, totalCount);
        List<ProductSummaryRes> pagedResults = visible.subList(fromIndex, toIndex).stream()
                .map(scored -> ProductSummaryRes.from(scored.product()))
                .toList();

        return ProductSearchRes.of(pagedResults, List.of(), false, page, size, totalCount);
    }

    @Transactional
    public ProductSearchRes searchExternal(String q) {
        if (q == null || q.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }

        String keyword = q.trim();

        String queryHash = HashUtil.sha256(keyword);
        SearchCachePayload cached = getSearchCache(queryHash);
        if (cached != null) {
            return ProductSearchRes.of(List.of(), cached.externalResults(), cached.degraded(), 0, cached.externalResults().size(), cached.externalResults().size());
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

        return ProductSearchRes.of(List.of(), payload.externalResults(), payload.degraded(), 0, payload.externalResults().size(), payload.externalResults().size());
    }

    private Specification<Product> buildDbSearchSpecification(List<String> tokens) {
        Specification<Product> allowedMallSpecification = (root, query, cb) -> {
            var normalizedMall = cb.upper(cb.function("replace", String.class, root.get("mallName"), cb.literal(" "), cb.literal("")));
            return normalizedMall.in(ALLOWED_MALLS.stream().map(this::normalizeMallName).toList());
        };

        Specification<Product> tokenSpecification = tokens.stream()
                .map(token -> (Specification<Product>) (root, query, cb) -> {
                    String likeToken = "%" + token.toUpperCase(Locale.ROOT) + "%";
                    return cb.or(
                            cb.like(cb.upper(root.get("brand")), likeToken),
                            cb.like(cb.upper(root.get("title")), likeToken)
                    );
                })
                .filter(Objects::nonNull)
                .reduce(Specification::or)
                .orElse((root, query, cb) -> cb.disjunction());

        return Specification.where(allowedMallSpecification).and(tokenSpecification);
    }

    private int scoreProduct(Product product, String keyword, List<String> tokens) {
        String normalizedQuery = normalizeText(keyword);
        String normalizedBrand = normalizeText(product.getBrand());
        String normalizedTitle = normalizeText(product.getTitle());

        int score = 0;
        if (!normalizedQuery.isBlank() && normalizedTitle.contains(normalizedQuery)) {
            score += 120;
        }
        if (!normalizedQuery.isBlank() && normalizedBrand.contains(normalizedQuery)) {
            score += 90;
        }

        for (String token : tokens) {
            if (normalizedBrand.contains(token)) {
                score += 45;
            }
            if (normalizedTitle.contains(token)) {
                score += 25;
            }
        }

        return score;
    }

    private List<String> tokenize(String keyword) {
        return Arrays.stream(normalizeText(keyword).split(" "))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .distinct()
                .toList();
    }

    private List<ProductSummaryRes> mapExternalItems(List<NaverShoppingItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<ProductSummaryRes> results = new ArrayList<>();
        Set<String> dedupe = new HashSet<>();

        for (NaverShoppingItem item : items) {
            String mallName = safe(item.mallName());
            if (!isAllowedMall(mallName)) {
                continue;
            }

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
                    mallName,
                    naverProductId.isBlank() ? null : naverProductId,
                    externalKey,
                    normalizedLink,
                    safe(item.image()),
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

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return NORMALIZE_PATTERN.matcher(value)
                .replaceAll(" ")
                .trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isAllowedMall(String mallName) {
        if (mallName == null || mallName.isBlank()) {
            return false;
        }

        String normalizedMallName = normalizeMallName(mallName);
        return ALLOWED_MALLS.stream()
                .map(this::normalizeMallName)
                .anyMatch(normalizedMallName::equals);
    }

    private String normalizeMallName(String mallName) {
        return mallName.trim()
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
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

    private record ScoredProduct(
            Product product,
            int score
    ) {
    }
}
