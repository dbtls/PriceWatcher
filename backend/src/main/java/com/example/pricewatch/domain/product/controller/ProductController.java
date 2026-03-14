package com.example.pricewatch.domain.product.controller;

import com.example.pricewatch.domain.product.dto.ProductSelectReq;
import com.example.pricewatch.domain.product.dto.ProductSelectRes;
import com.example.pricewatch.domain.product.dto.ProductListRes;
import com.example.pricewatch.domain.product.dto.ProductSearchRes;
import com.example.pricewatch.domain.product.dto.ProductSummaryRes;
import com.example.pricewatch.domain.task.service.ProductSearchIndexService;
import com.example.pricewatch.domain.product.service.ProductBrowseService;
import com.example.pricewatch.domain.product.service.ProductSearchService;
import com.example.pricewatch.domain.product.service.ProductService;
import com.example.pricewatch.global.dto.ResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 상품 API 컨트롤러.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {

    private final ProductSearchService productSearchService;
    private final ProductService productService;
    private final ProductSearchIndexService productSearchIndexService;
    private final ProductBrowseService productBrowseService;

    /**
     * 상품 검색(DB 전용).
     */
    @GetMapping("/search")
    public ResponseEntity<ResponseDto<ProductSearchRes>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ResponseDto.success("DB 검색 성공", productSearchService.searchDb(q, page, size)));
    }

    @GetMapping("/search/external")
    public ResponseEntity<ResponseDto<ProductSearchRes>> searchExternal(@RequestParam String q) {
        return ResponseEntity.ok(ResponseDto.success("외부 검색 성공", productSearchService.searchExternal(q)));
    }

    @PostMapping("/select")
    public ResponseEntity<ResponseDto<ProductSelectRes>> select(@Valid @RequestBody ProductSelectReq req) {
        return ResponseEntity.ok(ResponseDto.success("상품 선택 저장 성공", productService.select(req)));
    }

    /**
     * 상품 단일 상세 조회(디버깅/보조 조회용).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<ProductSummaryRes>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ResponseDto.success("상품 조회 성공", productService.getProduct(id)));
    }

    @GetMapping("/latest")
    public ResponseEntity<ResponseDto<ProductListRes>> latest(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ResponseDto.success("최신 상품 조회 성공", productBrowseService.getLatest(page, size)));
    }

    @GetMapping("/categories/{categoryId}")
    public ResponseEntity<ResponseDto<ProductListRes>> byCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ResponseDto.success("카테고리 상품 조회 성공", productBrowseService.getByCategory(categoryId, page, size)));
    }

    @PostMapping("/search-index/rebuild")
    public ResponseEntity<ResponseDto<Void>> rebuildSearchIndex() {
        productSearchIndexService.reindexAllProducts();
        return ResponseEntity.ok(ResponseDto.success("상품 검색 인덱스 재색인 요청 성공"));
    }
}
