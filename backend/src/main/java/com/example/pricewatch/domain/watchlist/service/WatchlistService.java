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

@Service
@RequiredArgsConstructor
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void add(Long userId, Long productId) {
        if (watchlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ApiException(ErrorCode.WATCHLIST_CONFLICT);
        }

        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        Product product = productRepository.findById(productId).orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
        watchlistRepository.save(Watchlist.builder().user(user).product(product).build());
    }

    @Transactional
    public void remove(Long userId, Long productId) {
        Watchlist watchlist = watchlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
        watchlistRepository.delete(watchlist);
    }

    @Transactional
    public void updateTargetPrice(Long userId, Long productId, BigDecimal targetPrice) {
        Watchlist watchlist = watchlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
        watchlist.updateTargetPrice(targetPrice);
    }

    @Transactional(readOnly = true)
    public List<WatchlistRes> getMine(Long userId) {
        return watchlistRepository.findByUserId(userId).stream().map(WatchlistRes::from).toList();
    }
}
