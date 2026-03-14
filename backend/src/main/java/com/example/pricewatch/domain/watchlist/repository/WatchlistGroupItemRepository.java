package com.example.pricewatch.domain.watchlist.repository;

import com.example.pricewatch.domain.watchlist.entity.WatchlistGroupItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchlistGroupItemRepository extends JpaRepository<WatchlistGroupItem, Long> {
    boolean existsByGroupIdAndProductId(Long groupId, Long productId);
    List<WatchlistGroupItem> findByGroupIdOrderByIdAsc(Long groupId);
    long countByGroupId(Long groupId);
    void deleteByGroupIdAndProductId(Long groupId, Long productId);
    void deleteByGroupId(Long groupId);
}
