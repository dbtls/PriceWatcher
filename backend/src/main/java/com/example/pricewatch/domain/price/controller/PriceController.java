package com.example.pricewatch.domain.price.controller;

import com.example.pricewatch.domain.price.dto.PriceHistoryItemRes;
import com.example.pricewatch.domain.price.service.PriceHistoryService;
import com.example.pricewatch.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 가격 이력 API 컨트롤러.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class PriceController {

    private final PriceHistoryService priceHistoryService;

    /**
     * 상품 가격 이력 조회.
     */
    @GetMapping("/{id}/price-history")
    public ResponseEntity<ResponseDto<List<PriceHistoryItemRes>>> priceHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(ResponseDto.success("가격 이력 조회 성공", priceHistoryService.getPriceHistory(id, days)));
    }
}
