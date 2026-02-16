package com.example.pricewatch.domain.repository;


import com.example.pricewatch.domain.model.WatchItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WatchItemRepository extends JpaRepository<WatchItem,Long> {
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    Optional<WatchItem> findByUserIdAndProductId(Long userId, Long productId);

}