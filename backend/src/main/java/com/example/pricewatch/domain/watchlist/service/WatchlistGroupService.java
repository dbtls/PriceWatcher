package com.example.pricewatch.domain.watchlist.service;

import com.example.pricewatch.domain.notification.entity.NotificationType;
import com.example.pricewatch.domain.notification.service.NotificationService;
import com.example.pricewatch.domain.price.dto.PriceHistoryItemRes;
import com.example.pricewatch.domain.price.service.PriceHistoryService;
import com.example.pricewatch.domain.product.entity.Product;
import com.example.pricewatch.domain.product.repository.ProductRepository;
import com.example.pricewatch.domain.user.entity.User;
import com.example.pricewatch.domain.user.repository.UserRepository;
import com.example.pricewatch.domain.watchlist.dto.*;
import com.example.pricewatch.domain.watchlist.entity.Watchlist;
import com.example.pricewatch.domain.watchlist.entity.WatchlistGroup;
import com.example.pricewatch.domain.watchlist.entity.WatchlistGroupItem;
import com.example.pricewatch.domain.watchlist.repository.WatchlistGroupItemRepository;
import com.example.pricewatch.domain.watchlist.repository.WatchlistGroupRepository;
import com.example.pricewatch.domain.watchlist.repository.WatchlistRepository;
import com.example.pricewatch.global.exception.ApiException;
import com.example.pricewatch.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WatchlistGroupService {

    private final WatchlistGroupRepository watchlistGroupRepository;
    private final WatchlistGroupItemRepository watchlistGroupItemRepository;
    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final PriceHistoryService priceHistoryService;
    private final NotificationService notificationService;

    @Transactional
    public WatchlistGroupRes create(Long userId, String name) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        WatchlistGroup group = watchlistGroupRepository.save(WatchlistGroup.builder()
                .user(user)
                .name(normalizeName(name))
                .build());
        return toSummary(group, List.of());
    }

    @Transactional(readOnly = true)
    public List<WatchlistGroupRes> getMine(Long userId) {
        List<WatchlistGroup> groups = watchlistGroupRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return groups.stream()
                .map(group -> {
                    List<WatchlistGroupItem> items = watchlistGroupItemRepository.findByGroupIdOrderByIdAsc(group.getId());
                    return toSummary(group, items);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public WatchlistGroupDetailRes getDetail(Long userId, Long groupId, int days) {
        WatchlistGroup group = getOwnedGroup(userId, groupId);
        List<WatchlistGroupItem> items = watchlistGroupItemRepository.findByGroupIdOrderByIdAsc(groupId);
        Map<Long, Watchlist> watchlistMap = watchlistRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(watchlist -> watchlist.getProduct().getId(), Function.identity()));

        List<WatchlistGroupItemRes> itemResponses = items.stream()
                .map(item -> toDetailItem(item.getProduct(), watchlistMap.get(item.getProduct().getId()), days))
                .sorted(Comparator
                        .comparing(WatchlistGroupItemRes::currentPrice, Comparator.nullsLast(BigDecimal::compareTo))
                        .thenComparing(WatchlistGroupItemRes::mallName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        return new WatchlistGroupDetailRes(group.getId(), group.getName(), itemResponses.size(), itemResponses);
    }

    @Transactional
    public WatchlistGroupRes rename(Long userId, Long groupId, String name) {
        WatchlistGroup group = getOwnedGroup(userId, groupId);
        group.rename(normalizeName(name));
        List<WatchlistGroupItem> items = watchlistGroupItemRepository.findByGroupIdOrderByIdAsc(groupId);
        return toSummary(group, items);
    }

    @Transactional
    public void delete(Long userId, Long groupId) {
        WatchlistGroup group = getOwnedGroup(userId, groupId);
        watchlistGroupItemRepository.deleteByGroupId(groupId);
        watchlistGroupRepository.delete(group);
    }

    @Transactional
    public WatchlistGroupDetailRes addItem(Long userId, Long groupId, Long productId) {
        WatchlistGroup group = getOwnedGroup(userId, groupId);
        if (!watchlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ApiException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (watchlistGroupItemRepository.existsByGroupIdAndProductId(groupId, productId)) {
            throw new ApiException(ErrorCode.WATCHLIST_GROUP_ITEM_CONFLICT);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
        watchlistGroupItemRepository.save(WatchlistGroupItem.builder()
                .group(group)
                .product(product)
                .build());

        notificationService.create(
                group.getUser(),
                NotificationType.WATCHLIST_ADDED,
                "비교 그룹에 상품이 추가되었습니다: " + product.getTitle(),
                productId
        );

        return getDetail(userId, groupId, 30);
    }

    @Transactional
    public WatchlistGroupDetailRes removeItem(Long userId, Long groupId, Long productId) {
        getOwnedGroup(userId, groupId);
        watchlistGroupItemRepository.deleteByGroupIdAndProductId(groupId, productId);
        return getDetail(userId, groupId, 30);
    }

    private WatchlistGroupRes toSummary(WatchlistGroup group, List<WatchlistGroupItem> items) {
        List<WatchlistGroupPreviewItemRes> previewItems = items.stream()
                .limit(3)
                .map(item -> new WatchlistGroupPreviewItemRes(
                        item.getProduct().getId(),
                        item.getProduct().getTitle(),
                        item.getProduct().getImageUrl()
                ))
                .toList();

        return new WatchlistGroupRes(group.getId(), group.getName(), items.size(), previewItems);
    }

    private WatchlistGroupItemRes toDetailItem(Product product, Watchlist watchlist, int days) {
        List<PriceHistoryItemRes> history = priceHistoryService.getPriceHistory(product.getId(), days);
        BigDecimal lowestPrice = history.stream()
                .map(PriceHistoryItemRes::price)
                .filter(price -> price != null)
                .min(BigDecimal::compareTo)
                .orElse(product.getPrice());

        return new WatchlistGroupItemRes(
                product.getId(),
                product.getBrand(),
                product.getTitle(),
                product.getMallName(),
                product.getImageUrl(),
                product.getPrice(),
                watchlist == null ? null : watchlist.getTargetPrice(),
                lowestPrice,
                product.getUrl(),
                history
        );
    }

    private WatchlistGroup getOwnedGroup(Long userId, Long groupId) {
        WatchlistGroup group = watchlistGroupRepository.findById(groupId)
                .orElseThrow(() -> new ApiException(ErrorCode.WATCHLIST_GROUP_NOT_FOUND));
        if (!group.getUser().getId().equals(userId)) {
            throw new ApiException(ErrorCode.ACCESS_DENIED);
        }
        return group;
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return name.trim();
    }
}
