package com.example.pricewatch.domain.product.service;

import com.example.pricewatch.domain.category.entity.Category;
import com.example.pricewatch.domain.category.service.CategoryService;
import com.example.pricewatch.domain.price.entity.PriceHistory;
import com.example.pricewatch.domain.price.repository.PriceHistoryRepository;
import com.example.pricewatch.domain.product.dto.ProductSelectReq;
import com.example.pricewatch.domain.product.dto.ProductSelectRes;
import com.example.pricewatch.domain.product.dto.ProductSummaryRes;
import com.example.pricewatch.domain.product.entity.Product;
import com.example.pricewatch.domain.product.entity.RefreshStatus;
import com.example.pricewatch.domain.product.repository.ProductRepository;
import com.example.pricewatch.domain.task.service.ProductSearchSyncService;
import com.example.pricewatch.global.exception.ApiException;
import com.example.pricewatch.global.exception.ErrorCode;
import com.example.pricewatch.global.util.HashUtil;
import com.example.pricewatch.global.util.LinkNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ProductSearchSyncService productSearchSyncService;

    @Transactional(readOnly = true)
    public ProductSummaryRes getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductSummaryRes.from(product);
    }

    @Transactional
    public ProductSelectRes select(ProductSelectReq req) {
        if (req.productId() != null) {
            Product existingProduct = productRepository.findById(req.productId())
                    .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
            return ProductSelectRes.of(ProductSummaryRes.from(existingProduct), false);
        }

        String brand = normalizeRequired(req.brand());
        String title = normalizeRequired(req.title());
        String url = normalizeRequired(req.url());
        if (req.price() == null || req.price().signum() < 0) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String normalizedUrl = LinkNormalizer.normalize(url);
        String imageUrl = normalizeNullable(req.imageUrl());
        String naverProductId = normalizeNullable(req.naverProductId());
        String externalKey = normalizeNullable(req.externalKey());
        if (externalKey == null) {
            externalKey = HashUtil.sha256(normalizedUrl);
        }
        if (naverProductId == null && externalKey == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Category category = resolveCategory(req.categoryPath());
        LocalDateTime now = LocalDateTime.now();
        Optional<Product> existing = findExistingProduct(naverProductId, externalKey);

        if (existing.isPresent()) {
            Product product = existing.get();
            product.applySelection(
                    brand,
                    title,
                    category,
                    req.price(),
                    normalizeNullable(req.mallName()),
                    naverProductId,
                    externalKey == null ? product.getExternalKey() : externalKey,
                    normalizedUrl,
                    imageUrl,
                    now
            );
            upsertTodayPriceHistory(product, req.price(), now);
            productSearchSyncService.publishUpsert(product.getId());
            return ProductSelectRes.of(ProductSummaryRes.from(product), false);
        }

        Product product = Product.builder()
                .brand(brand)
                .title(title)
                .category(category)
                .price(req.price())
                .mallName(normalizeNullable(req.mallName()))
                .naverProductId(naverProductId)
                .externalKey(externalKey)
                .url(normalizedUrl)
                .imageUrl(imageUrl)
                .lastSeenAt(now)
                .refreshStatus(RefreshStatus.READY)
                .needsRematch(false)
                .failCount(0)
                .build();
        productRepository.save(product);
        upsertTodayPriceHistory(product, req.price(), now);
        productSearchSyncService.publishUpsert(product.getId());
        return ProductSelectRes.of(ProductSummaryRes.from(product), true);
    }

    private void upsertTodayPriceHistory(Product product, java.math.BigDecimal price, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        PriceHistory history = priceHistoryRepository.findByProductIdAndCapturedAt(product.getId(), today)
                .orElseGet(() -> PriceHistory.builder()
                        .product(product)
                        .price(price)
                        .capturedAt(today)
                        .createdAt(now)
                        .build());

        if (history.getId() == null) {
            priceHistoryRepository.save(history);
            return;
        }
        history.updatePrice(price);
    }

    private Optional<Product> findExistingProduct(String naverProductId, String externalKey) {
        if (naverProductId != null) {
            Optional<Product> byNaverProductId = productRepository.findByNaverProductId(naverProductId);
            if (byNaverProductId.isPresent()) {
                return byNaverProductId;
            }
        }
        if (externalKey != null) {
            return productRepository.findByExternalKey(externalKey);
        }
        return Optional.empty();
    }

    private Category resolveCategory(String categoryPath) {
        String normalizedPath = normalizeNullable(categoryPath);
        if (normalizedPath == null) {
            return null;
        }

        List<String> segments = Arrays.stream(normalizedPath.split(">"))
                .map(String::trim)
                .filter(segment -> !segment.isBlank())
                .toList();
        if (segments.isEmpty()) {
            return null;
        }
        return categoryService.resolveLeaf(segments);
    }

    private String normalizeRequired(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
