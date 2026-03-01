package com.example.pricewatch.domain.price.service;

import com.example.pricewatch.domain.price.dto.PriceHistoryItemRes;
import com.example.pricewatch.domain.price.repository.PriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 가격 이력 조회 서비스.
 */
@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;

    /**
     * 기간 기준 가격 이력 조회.
     */
    @Transactional(readOnly = true)
    public List<PriceHistoryItemRes> getPriceHistory(Long productId, int days) {
        LocalDate fromDate = LocalDate.now().minusDays(Math.max(days, 1));
        return priceHistoryRepository.findByProductIdAndCapturedAtGreaterThanEqualOrderByCapturedAtDesc(productId, fromDate)
                .stream()
                .map(PriceHistoryItemRes::from)
                .toList();
    }
}
