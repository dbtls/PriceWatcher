package com.example.pricewatch.domain.user.service;

import com.example.pricewatch.domain.user.dto.ProfileRes;
import com.example.pricewatch.domain.user.dto.MyPageRecentDropRes;
import com.example.pricewatch.domain.user.dto.MyPageSummaryRes;
import com.example.pricewatch.domain.user.entity.User;
import com.example.pricewatch.domain.user.repository.UserRepository;
import com.example.pricewatch.domain.watchlist.entity.Watchlist;
import com.example.pricewatch.domain.watchlist.repository.WatchlistGroupRepository;
import com.example.pricewatch.domain.watchlist.repository.WatchlistRepository;
import com.example.pricewatch.domain.notification.repository.NotificationRepository;
import com.example.pricewatch.domain.price.dto.PriceHistoryItemRes;
import com.example.pricewatch.domain.price.service.PriceHistoryService;
import com.example.pricewatch.global.exception.ApiException;
import com.example.pricewatch.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 도메인 서비스.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WatchlistRepository watchlistRepository;
    private final WatchlistGroupRepository watchlistGroupRepository;
    private final NotificationRepository notificationRepository;
    private final PriceHistoryService priceHistoryService;

    /**
     * 내 프로필 조회.
     */
    @Transactional(readOnly = true)
    public ProfileRes getMyProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        return ProfileRes.from(user);
    }

    @Transactional(readOnly = true)
    public MyPageSummaryRes getMyPageSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        long watchlistCount = watchlistRepository.countByUserId(userId);
        long watchlistGroupCount = watchlistGroupRepository.countByUserId(userId);
        long unreadNotificationCount = notificationRepository.countByUserIdAndIsReadFalse(userId);

        java.util.List<Watchlist> watchlists = watchlistRepository.findByUserId(userId);
        long targetReachedCount = watchlists.stream()
                .filter(watchlist -> watchlist.getTargetPrice() != null)
                .filter(watchlist -> watchlist.getProduct().getPrice() != null)
                .filter(watchlist -> watchlist.getProduct().getPrice().compareTo(watchlist.getTargetPrice()) <= 0)
                .count();

        java.util.List<MyPageRecentDropRes> recentDrops = watchlists.stream()
                .map(this::toRecentDrop)
                .filter(java.util.Objects::nonNull)
                .sorted((left, right) -> Integer.compare(right.dropRatePercent(), left.dropRatePercent()))
                .limit(5)
                .toList();

        return new MyPageSummaryRes(
                ProfileRes.from(user),
                watchlistCount,
                watchlistGroupCount,
                unreadNotificationCount,
                targetReachedCount,
                recentDrops
        );
    }

    private MyPageRecentDropRes toRecentDrop(Watchlist watchlist) {
        java.util.List<PriceHistoryItemRes> history = priceHistoryService.getPriceHistory(watchlist.getProduct().getId(), 7);
        if (history.size() < 2) {
            return null;
        }

        java.math.BigDecimal currentPrice = history.get(0).price();
        java.math.BigDecimal previousPrice = history.get(history.size() - 1).price();
        if (currentPrice == null || previousPrice == null || previousPrice.signum() <= 0) {
            return null;
        }
        if (currentPrice.compareTo(previousPrice) >= 0) {
            return null;
        }

        java.math.BigDecimal dropAmount = previousPrice.subtract(currentPrice);
        int dropRatePercent = dropAmount
                .multiply(java.math.BigDecimal.valueOf(100))
                .divide(previousPrice, 0, java.math.RoundingMode.HALF_UP)
                .intValue();

        return new MyPageRecentDropRes(
                watchlist.getProduct().getId(),
                watchlist.getProduct().getTitle(),
                watchlist.getProduct().getImageUrl(),
                currentPrice,
                previousPrice,
                dropAmount,
                dropRatePercent
        );
    }
}
