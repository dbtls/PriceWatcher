package com.example.pricewatch.domain.watchlist.controller;

import com.example.pricewatch.domain.watchlist.dto.UpdateTargetPriceReq;
import com.example.pricewatch.domain.watchlist.dto.WatchlistRes;
import com.example.pricewatch.domain.watchlist.dto.AddWatchlistGroupItemReq;
import com.example.pricewatch.domain.watchlist.dto.CreateWatchlistGroupReq;
import com.example.pricewatch.domain.watchlist.dto.RenameWatchlistGroupReq;
import com.example.pricewatch.domain.watchlist.dto.WatchlistGroupDetailRes;
import com.example.pricewatch.domain.watchlist.dto.WatchlistGroupRes;
import com.example.pricewatch.domain.watchlist.service.WatchlistGroupService;
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
    private final WatchlistGroupService watchlistGroupService;

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

    @PostMapping("/groups")
    public ResponseEntity<ResponseDto<WatchlistGroupRes>> createGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CreateWatchlistGroupReq req
    ) {
        return ResponseEntity.ok(ResponseDto.success("워치리스트 그룹 생성 성공", watchlistGroupService.create(principal.getUserId(), req.name())));
    }

    @GetMapping("/groups")
    public ResponseEntity<ResponseDto<List<WatchlistGroupRes>>> myGroups(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ResponseDto.success("워치리스트 그룹 조회 성공", watchlistGroupService.getMine(principal.getUserId())));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<ResponseDto<WatchlistGroupDetailRes>> groupDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(ResponseDto.success("워치리스트 그룹 상세 조회 성공", watchlistGroupService.getDetail(principal.getUserId(), groupId, days)));
    }

    @PatchMapping("/groups/{groupId}")
    public ResponseEntity<ResponseDto<WatchlistGroupRes>> renameGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long groupId,
            @RequestBody RenameWatchlistGroupReq req
    ) {
        return ResponseEntity.ok(ResponseDto.success("워치리스트 그룹 이름 변경 성공", watchlistGroupService.rename(principal.getUserId(), groupId, req.name())));
    }

    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<ResponseDto<Void>> deleteGroup(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long groupId
    ) {
        watchlistGroupService.delete(principal.getUserId(), groupId);
        return ResponseEntity.ok(ResponseDto.success("워치리스트 그룹 삭제 성공"));
    }

    @PostMapping("/groups/{groupId}/items")
    public ResponseEntity<ResponseDto<WatchlistGroupDetailRes>> addGroupItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long groupId,
            @RequestBody AddWatchlistGroupItemReq req
    ) {
        return ResponseEntity.ok(ResponseDto.success("워치리스트 그룹 상품 추가 성공", watchlistGroupService.addItem(principal.getUserId(), groupId, req.productId())));
    }

    @DeleteMapping("/groups/{groupId}/items/{productId}")
    public ResponseEntity<ResponseDto<WatchlistGroupDetailRes>> removeGroupItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long groupId,
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(ResponseDto.success("워치리스트 그룹 상품 제거 성공", watchlistGroupService.removeItem(principal.getUserId(), groupId, productId)));
    }
}
