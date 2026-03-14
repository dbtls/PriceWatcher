package com.example.pricewatch.domain.product.service;

import com.example.pricewatch.domain.category.entity.Category;
import com.example.pricewatch.domain.category.repository.CategoryRepository;
import com.example.pricewatch.domain.product.dto.ProductListRes;
import com.example.pricewatch.domain.product.dto.ProductSummaryRes;
import com.example.pricewatch.domain.product.repository.ProductRepository;
import com.example.pricewatch.global.exception.ApiException;
import com.example.pricewatch.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductBrowseService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public ProductListRes getLatest(int page, int size) {
        PageRequest pageable = toPageable(page, size);
        Page<com.example.pricewatch.domain.product.entity.Product> products = productRepository.findAllByOrderByCreatedAtDesc(pageable);
        return ProductListRes.of(
                products.getContent().stream().map(ProductSummaryRes::from).toList(),
                page,
                size,
                products.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public ProductListRes getByCategory(Long categoryId, int page, int size) {
        if (categoryId == null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_INPUT_VALUE));
        PageRequest pageable = toPageable(page, size);
        Page<com.example.pricewatch.domain.product.entity.Product> products =
                productRepository.findByCategoryTree(category.getId(), category.getPath(), pageable);

        return ProductListRes.of(
                products.getContent().stream().map(ProductSummaryRes::from).toList(),
                page,
                size,
                products.getTotalElements()
        );
    }

    private PageRequest toPageable(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return PageRequest.of(page, size);
    }
}
