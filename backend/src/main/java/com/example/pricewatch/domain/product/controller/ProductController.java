package com.example.pricewatch.domain.product.controller;

import com.example.pricewatch.domain.product.dto.ProductSearchRes;
import com.example.pricewatch.domain.product.dto.ProductSummaryRes;
import com.example.pricewatch.domain.product.service.ProductSearchService;
import com.example.pricewatch.domain.product.service.ProductService;
import com.example.pricewatch.global.dto.ResponseDto;
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

    /**
     * 상품 검색.
     */
    @GetMapping("/search")
    public ResponseEntity<ResponseDto<ProductSearchRes>> search(@RequestParam String q) {
        return ResponseEntity.ok(ResponseDto.success("검색 성공", productSearchService.search(q)));
    }

    /**
     * 상품 상세 조회.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<ProductSummaryRes>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ResponseDto.success("상품 조회 성공", productService.getProduct(id)));
    }
}
