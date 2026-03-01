package com.example.pricewatch.domain.recommendation.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 핫 랭킹 서비스.
 */
@Service
public class RankingService {

    /**
     * 일간 랭킹 조회.
     */
    public List<Long> getDailyRank() {
        return List.of();
    }

    /**
     * 주간 랭킹 조회.
     */
    public List<Long> getWeeklyRank() {
        return List.of();
    }
}
