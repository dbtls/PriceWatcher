package com.example.pricewatch.domain.watchlist.repository;

import com.example.pricewatch.domain.watchlist.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 워치리스트 저장소.
 */
public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    long countByUserId(Long userId);
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    Optional<Watchlist> findByUserIdAndProductId(Long userId, Long productId);
    List<Watchlist> findByUserId(Long userId);
    List<Watchlist> findByProductId(Long productId);
}
