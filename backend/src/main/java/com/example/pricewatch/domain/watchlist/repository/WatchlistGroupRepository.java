package com.example.pricewatch.domain.watchlist.repository;

import com.example.pricewatch.domain.watchlist.entity.WatchlistGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchlistGroupRepository extends JpaRepository<WatchlistGroup, Long> {
    long countByUserId(Long userId);
    List<WatchlistGroup> findByUserIdOrderByCreatedAtDesc(Long userId);
}
