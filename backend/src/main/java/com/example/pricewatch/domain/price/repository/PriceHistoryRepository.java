package com.example.pricewatch.domain.price.repository;

import com.example.pricewatch.domain.price.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 가격 이력 저장소.
 */
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    List<PriceHistory> findByProductIdAndCapturedAtGreaterThanEqualOrderByCapturedAtDesc(Long productId, LocalDate fromDate);
    Optional<PriceHistory> findByProductIdAndCapturedAt(Long productId, LocalDate capturedAt);
    boolean existsByProductIdAndPriceLessThanEqual(Long productId, BigDecimal price);
}
