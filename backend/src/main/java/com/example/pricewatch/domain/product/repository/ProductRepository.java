package com.example.pricewatch.domain.product.repository;

import com.example.pricewatch.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 상품 저장소.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByNaverProductId(String naverProductId);
    Optional<Product> findByExternalKey(String externalKey);
    List<Product> findTop5ByBrandContainingIgnoreCaseOrTitleContainingIgnoreCase(String brand, String title);
}
