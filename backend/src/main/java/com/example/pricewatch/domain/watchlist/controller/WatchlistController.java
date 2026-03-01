package com.example.pricewatch.domain.watchlist.controller;

import com.example.pricewatch.domain.watchlist.dto.UpdateTargetPriceReq;
import com.example.pricewatch.domain.watchlist.dto.WatchlistRes;
import com.example.pricewatch.domain.watchlist.service.WatchlistService;
import com.example.pricewatch.global.dto.ResponseDto;
import com.example.pricewatch.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 워치리스트 API 컨트롤러.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    /**
     * 워치리스트 상품 추가.
     */
    @PostMapping("/{productId}")
    public ResponseEntity<ResponseDto<Void>> add(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long productId) {
        watchlistService.add(principal.getUserId(), productId);
        return ResponseEntity.ok(ResponseDto.success("워치리스트 등록 성공"));
    }

    /**
     * 워치리스트 상품 제거.
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ResponseDto<Void>> remove(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long productId) {
        watchlistService.remove(principal.getUserId(), productId);
        return ResponseEntity.ok(ResponseDto.success("워치리스트 삭제 성공"));
    }

    /**
     * 워치리스트 목표가 변경.
     */
    @PatchMapping("/{productId}")
    public ResponseEntity<ResponseDto<Void>> updateTargetPrice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long productId,
            @RequestBody UpdateTargetPriceReq req
    ) {
        watchlistService.updateTargetPrice(principal.getUserId(), productId, req.targetPrice());
        return ResponseEntity.ok(ResponseDto.success("목표가 설정 성공"));
    }

    /**
     * 내 워치리스트 목록 조회.
     */
    @GetMapping
    public ResponseEntity<ResponseDto<List<WatchlistRes>>> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ResponseDto.success("워치리스트 조회 성공", watchlistService.getMine(principal.getUserId())));
    }
}
