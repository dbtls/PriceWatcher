package com.example.pricewatch.domain.product.service;

import com.example.pricewatch.domain.product.dto.ProductSummaryRes;
import com.example.pricewatch.domain.product.entity.Product;
import com.example.pricewatch.domain.product.repository.ProductRepository;
import com.example.pricewatch.global.exception.ApiException;
import com.example.pricewatch.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 조회 서비스.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 상품 단건 조회.
     */
    @Transactional(readOnly = true)
    public ProductSummaryRes getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(ErrorCode.PRODUCT_NOT_FOUND));
        return ProductSummaryRes.from(product);
    }
}
