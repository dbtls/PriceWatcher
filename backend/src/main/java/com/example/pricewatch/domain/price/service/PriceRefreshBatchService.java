package com.example.pricewatch.domain.price.service;

import com.example.pricewatch.domain.notification.entity.NotificationType;
import com.example.pricewatch.domain.notification.service.NotificationService;
import com.example.pricewatch.domain.price.batch.ProductBrandMallGroup;
import com.example.pricewatch.domain.price.dto.PriceRefreshBatchSummary;
import com.example.pricewatch.domain.price.entity.PriceHistory;
import com.example.pricewatch.domain.price.repository.PriceHistoryRepository;
import com.example.pricewatch.domain.product.client.NaverShoppingClient;
import com.example.pricewatch.domain.product.client.dto.NaverShoppingItem;
import com.example.pricewatch.domain.product.client.dto.NaverShoppingSearchResponse;
import com.example.pricewatch.domain.product.entity.Product;
import com.example.pricewatch.domain.product.entity.RefreshStatus;
import com.example.pricewatch.domain.product.repository.ProductRepository;
import com.example.pricewatch.domain.task.service.ProductSearchSyncService;
import com.example.pricewatch.domain.watchlist.entity.Watchlist;
import com.example.pricewatch.domain.watchlist.repository.WatchlistRepository;
import com.example.pricewatch.global.util.HashUtil;
import com.example.pricewatch.global.util.LinkNormalizer;
import com.example.pricewatch.global.util.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceRefreshBatchService {

    private static final List<String> ALLOWED_MALLS = List.of("무신사", "29CM", "하이츠스토어", "EQL");

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final WatchlistRepository watchlistRepository;
    private final NotificationService notificationService;
    private final NaverShoppingClient naverShoppingClient;
    private final StringRedisTemplate redisTemplate;
    private final ProductSearchSyncService productSearchSyncService;

    @Value("${naver.shopping.daily-quota:20000}")
    private long naverDailyQuota;
    @Value("${naver.shopping.display:100}")
    private int naverDisplay;
    @Value("${app.batch.price.group-page-size:500}")
    private int groupPageSize;
    @Value("${app.batch.price.second-pass-size:1000}")
    private int secondPassSize;
    @Value("${app.notification.price-drop-rate-threshold:10}")
    private int priceDropRateThresholdPercent;

    @Transactional
    public PriceRefreshBatchSummary runFirstPass(LocalDate batchDate, LocalDateTime batchStartedAt) {
        BatchSummaryAccumulator summary = new BatchSummaryAccumulator();
        List<ProductBrandMallGroup> groups = productRepository.findRefreshGroups(PageRequest.of(0, groupPageSize))
                .stream()
                .filter(group -> isAllowedMall(group.mallName()))
                .toList();
        summary.firstPassGroups = groups.size();

        for (ProductBrandMallGroup group : groups) {
            if (!tryConsumeNaverQuota()) {
                log.warn("First pass stopped because Naver daily quota was exhausted");
                summary.quotaExhausted = true;
                return summary.toSummary();
            }

            List<Product> products = productRepository.findByMallNameIgnoreCaseAndBrandIgnoreCase(group.mallName(), group.brand());
            if (products.isEmpty()) {
                continue;
            }

            try {
                String query = group.mallName() + " " + group.brand();
                summary.firstPassApiCalls += 1;
                NaverShoppingSearchResponse response = naverShoppingClient.search(query, naverDisplay);
                applyGroupMatches(products, response.items(), batchDate, batchStartedAt, summary);
            } catch (Exception e) {
                log.error("First pass group refresh failed. mall={}, brand={}", group.mallName(), group.brand(), e);
                products.forEach(product -> product.markRefreshFailed(RefreshStatus.FAILED));
                summary.failed += products.size();
            }
        }
        return summary.toSummary();
    }

    @Transactional
    public PriceRefreshBatchSummary runSecondPass(LocalDate batchDate, LocalDateTime batchStartedAt) {
        BatchSummaryAccumulator summary = new BatchSummaryAccumulator();
        List<Product> candidates = productRepository.findPendingRefreshCandidates(batchStartedAt, PageRequest.of(0, secondPassSize));
        summary.secondPassCandidates = candidates.size();
        for (Product product : candidates) {
            if (!isAllowedMall(product.getMallName())) {
                continue;
            }
            if (!tryConsumeNaverQuota()) {
                log.warn("Second pass stopped because Naver daily quota was exhausted");
                summary.quotaExhausted = true;
                return summary.toSummary();
            }

            try {
                String query = buildProductQuery(product);
                summary.secondPassApiCalls += 1;
                NaverShoppingSearchResponse response = naverShoppingClient.search(query, naverDisplay);
                matchSingleProduct(product, response.items(), batchDate, summary);
            } catch (Exception e) {
                log.error("Second pass product refresh failed. productId={}", product.getId(), e);
                product.markRefreshFailed(RefreshStatus.FAILED);
                summary.failed += 1;
            }
        }
        return summary.toSummary();
    }

    private void applyGroupMatches(
            List<Product> products,
            List<NaverShoppingItem> items,
            LocalDate batchDate,
            LocalDateTime batchStartedAt,
            BatchSummaryAccumulator summary
    ) {
        Map<String, Product> byNaverProductId = new HashMap<>();
        Map<String, Product> byExternalKey = new HashMap<>();

        for (Product product : products) {
            if (product.getNaverProductId() != null && !product.getNaverProductId().isBlank()) {
                byNaverProductId.put(product.getNaverProductId(), product);
            }
            byExternalKey.put(product.getExternalKey(), product);
        }

        for (NaverShoppingItem item : items) {
            if (!isAllowedMall(item.mallName())) {
                continue;
            }

            String naverProductId = safe(item.productId());
            String externalKey = HashUtil.sha256(LinkNormalizer.normalize(safe(item.link())));
            Product matched = !naverProductId.isBlank() ? byNaverProductId.get(naverProductId) : null;
            if (matched == null) {
                matched = byExternalKey.get(externalKey);
            }
            if (matched == null) {
                continue;
            }

            updateProductPrice(matched, parsePrice(item.lprice()), batchDate, summary, true);
        }

        products.stream()
                .filter(product -> product.getLastSeenAt() == null || product.getLastSeenAt().isBefore(batchStartedAt))
                .forEach(product -> {
                    product.markRefreshFailed(RefreshStatus.NOT_FOUND);
                    summary.failed += 1;
                });
    }

    private void matchSingleProduct(Product product, List<NaverShoppingItem> items, LocalDate batchDate, BatchSummaryAccumulator summary) {
        NaverShoppingItem matchedItem = null;
        boolean sameMallCandidateExists = false;

        for (NaverShoppingItem item : items) {
            if (!sameMall(product.getMallName(), item.mallName())) {
                continue;
            }
            sameMallCandidateExists = true;

            String naverProductId = safe(item.productId());
            String externalKey = HashUtil.sha256(LinkNormalizer.normalize(safe(item.link())));
            boolean idMatch = product.getNaverProductId() != null && !product.getNaverProductId().isBlank()
                    && product.getNaverProductId().equals(naverProductId);
            boolean externalKeyMatch = product.getExternalKey().equals(externalKey);
            if (idMatch || externalKeyMatch) {
                matchedItem = item;
                break;
            }
        }

        if (matchedItem == null) {
            product.markRefreshFailed(sameMallCandidateExists ? RefreshStatus.MISMATCH : RefreshStatus.NOT_FOUND);
            summary.failed += 1;
            return;
        }

        updateProductPrice(product, parsePrice(matchedItem.lprice()), batchDate, summary, false);
    }

    private void updateProductPrice(Product product, BigDecimal latestPrice, LocalDate batchDate, BatchSummaryAccumulator summary, boolean firstPass) {
        BigDecimal previousPrice = product.getPrice();
        boolean newLowest = !priceHistoryRepository.existsByProductIdAndPriceLessThanEqual(product.getId(), latestPrice);

        product.refresh(latestPrice, LocalDateTime.now());
        savePriceHistory(product, latestPrice, batchDate);
        triggerNotifications(product, previousPrice, latestPrice, newLowest, summary);
        productSearchSyncService.publishUpsert(product.getId());
        if (firstPass) {
            summary.firstPassUpdated += 1;
        } else {
            summary.secondPassUpdated += 1;
        }
    }

    private void savePriceHistory(Product product, BigDecimal latestPrice, LocalDate batchDate) {
        PriceHistory history = priceHistoryRepository.findByProductIdAndCapturedAt(product.getId(), batchDate)
                .orElseGet(() -> PriceHistory.builder()
                        .product(product)
                        .price(latestPrice)
                        .capturedAt(batchDate)
                        .createdAt(LocalDateTime.now())
                        .build());

        if (history.getId() == null) {
            priceHistoryRepository.save(history);
        } else {
            history.updatePrice(latestPrice);
        }
    }

    private void triggerNotifications(Product product, BigDecimal previousPrice, BigDecimal latestPrice, boolean newLowest, BatchSummaryAccumulator summary) {
        List<Watchlist> watchlists = watchlistRepository.findByProductId(product.getId());
        for (Watchlist watchlist : watchlists) {
            if (watchlist.getTargetPrice() != null
                    && latestPrice.compareTo(watchlist.getTargetPrice()) <= 0
                    && (previousPrice == null || previousPrice.compareTo(watchlist.getTargetPrice()) > 0)) {
                notificationService.create(
                        watchlist.getUser(),
                        NotificationType.TARGET_PRICE_REACHED,
                        product.getTitle() + " 상품이 목표가 이하로 내려갔습니다: " + latestPrice,
                        product.getId()
                );
                summary.targetPriceNotifications += 1;
            }

            if (newLowest) {
                notificationService.create(
                        watchlist.getUser(),
                        NotificationType.NEW_LOWEST_PRICE,
                        product.getTitle() + " 상품이 최저가를 갱신했습니다: " + latestPrice,
                        product.getId()
                );
                summary.newLowestNotifications += 1;
            }

            if (previousPrice != null
                    && previousPrice.signum() > 0
                    && latestPrice.compareTo(previousPrice) < 0
                    && calculateDropRate(previousPrice, latestPrice) >= priceDropRateThresholdPercent) {
                notificationService.create(
                        watchlist.getUser(),
                        NotificationType.PRICE_DROP_RATE,
                        product.getTitle() + " 상품 가격이 " + calculateDropRate(previousPrice, latestPrice) + "% 하락했습니다.",
                        product.getId()
                );
                summary.priceDropNotifications += 1;
            }
        }
    }

    private int calculateDropRate(BigDecimal previousPrice, BigDecimal latestPrice) {
        BigDecimal drop = previousPrice.subtract(latestPrice);
        return drop.multiply(BigDecimal.valueOf(100))
                .divide(previousPrice, 0, RoundingMode.DOWN)
                .intValue();
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

    private String buildProductQuery(Product product) {
        return product.getMallName() + " " + product.getBrand() + " " + product.getTitle();
    }

    private boolean isAllowedMall(String mallName) {
        if (mallName == null || mallName.isBlank()) {
            return false;
        }

        String normalized = normalizeMallName(mallName);
        return ALLOWED_MALLS.stream()
                .map(this::normalizeMallName)
                .anyMatch(normalized::equals);
    }

    private boolean sameMall(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return normalizeMallName(left).equals(normalizeMallName(right));
    }

    private String normalizeMallName(String mallName) {
        return mallName.trim()
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
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

    private static class BatchSummaryAccumulator {
        private int firstPassGroups;
        private int firstPassUpdated;
        private int firstPassApiCalls;
        private int secondPassCandidates;
        private int secondPassUpdated;
        private int secondPassApiCalls;
        private int failed;
        private int targetPriceNotifications;
        private int newLowestNotifications;
        private int priceDropNotifications;
        private boolean quotaExhausted;

        private PriceRefreshBatchSummary toSummary() {
            return new PriceRefreshBatchSummary(
                    firstPassGroups,
                    firstPassUpdated,
                    firstPassApiCalls,
                    secondPassCandidates,
                    secondPassUpdated,
                    secondPassApiCalls,
                    firstPassApiCalls + secondPassApiCalls,
                    failed,
                    targetPriceNotifications,
                    newLowestNotifications,
                    priceDropNotifications,
                    quotaExhausted
            );
        }
    }
}
