package com.example.pricewatch.domain.watchlist.service;

import com.example.pricewatch.domain.product.entity.Product;
import com.example.pricewatch.domain.product.repository.ProductRepository;
import com.example.pricewatch.domain.user.entity.User;
import com.example.pricewatch.domain.user.repository.UserRepository;
import com.example.pricewatch.domain.watchlist.dto.WatchlistRes;
import com.example.pricewatch.domain.watchlist.entity.Watchlist;
import com.example.pricewatch.domain.watchlist.repository.WatchlistRepository;
import com.example.pricewatch.global.exception.ApiException;
import com.example.pricewatch.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 워치리스트 서비스.
 */
@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /**
     * 워치리스트 상품 추가.
     */
    @Transactional
    public void add(Long userId, Long productId) {
        if (watchlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ApiException(ErrorCode.WATCHLIST_CONFLICT);
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findById(productId).orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
        watchlistRepository.save(Watchlist.of(user, product));
    }

    /**
     * 워치리스트 상품 제거.
     */
    @Transactional
    public void remove(Long userId, Long productId) {
        Watchlist watchlist = watchlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
        watchlistRepository.delete(watchlist);
    }

    /**
     * 워치리스트 목표가 변경.
     */
    @Transactional
    public void updateTargetPrice(Long userId, Long productId, BigDecimal targetPrice) {
        Watchlist watchlist = watchlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
        watchlist.updateTargetPrice(targetPrice);
    }

    /**
     * 내 워치리스트 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<WatchlistRes> getMine(Long userId) {
        return watchlistRepository.findByUserId(userId).stream().map(WatchlistRes::from).toList();
    }
}
