package com.example.pricewatch.domain.recommendation.controller;

import com.example.pricewatch.domain.recommendation.dto.ProductRecommendationsRes;
import com.example.pricewatch.domain.recommendation.service.RelatedProductService;
import com.example.pricewatch.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RecommendationController {

    private final RelatedProductService relatedProductService;

    @GetMapping("/products/{id}/recommendations")
    public ResponseEntity<ResponseDto<ProductRecommendationsRes>> recommendations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(ResponseDto.success("상품 추천 조회 성공", relatedProductService.getRecommendations(id, limit)));
    }
}
