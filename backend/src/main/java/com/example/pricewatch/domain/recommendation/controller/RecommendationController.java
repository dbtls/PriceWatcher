package com.example.pricewatch.domain.recommendation.controller;

import com.example.pricewatch.domain.recommendation.service.RankingService;
import com.example.pricewatch.domain.recommendation.service.RelatedProductService;
import com.example.pricewatch.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 추천/랭킹 API 컨트롤러.
 */
@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private final RelatedProductService relatedProductService;
    private final RankingService rankingService;

    /**
     * 연관 상품 조회.
     */
    @GetMapping("/products/{id}/related")
    public ResponseEntity<ResponseDto<List<Long>>> related(@PathVariable Long id) {
        return ResponseEntity.ok(ResponseDto.success("연관 상품 조회 성공", relatedProductService.getRelated(id, 10)));
    }

    /**
     * 일간 랭킹 조회.
     */
    @GetMapping("/rank/daily")
    public ResponseEntity<ResponseDto<List<Long>>> daily() {
        return ResponseEntity.ok(ResponseDto.success("일간 랭킹 조회 성공", rankingService.getDailyRank()));
    }

    /**
     * 주간 랭킹 조회.
     */
    @GetMapping("/rank/weekly")
    public ResponseEntity<ResponseDto<List<Long>>> weekly() {
        return ResponseEntity.ok(ResponseDto.success("주간 랭킹 조회 성공", rankingService.getWeeklyRank()));
    }
}
