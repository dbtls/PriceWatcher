package com.example.pricewatch.domain.price.controller;

import com.example.pricewatch.domain.price.batch.PriceRefreshJobRunner;
import com.example.pricewatch.domain.price.dto.PriceRefreshBatchSummary;
import com.example.pricewatch.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/batch/price")
public class PriceBatchController {

    private final PriceRefreshJobRunner priceRefreshJobRunner;

    @PostMapping("/run")
    public ResponseEntity<ResponseDto<PriceRefreshBatchSummary>> runNow() {
        PriceRefreshBatchSummary summary = priceRefreshJobRunner.runOnce();
        return ResponseEntity.ok(ResponseDto.success("가격 배치 수동 실행 성공", summary));
    }
}
