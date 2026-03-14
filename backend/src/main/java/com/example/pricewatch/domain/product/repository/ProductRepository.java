package com.example.pricewatch.domain.product.repository;

import com.example.pricewatch.domain.price.batch.ProductBrandMallGroup;
import com.example.pricewatch.domain.product.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 상품 저장소.
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findByNaverProductId(String naverProductId);
    Optional<Product> findByExternalKey(String externalKey);
    List<Product> findTop5ByBrandContainingIgnoreCaseOrTitleContainingIgnoreCase(String brand, String title);
    List<Product> findByMallNameIgnoreCaseAndBrandIgnoreCase(String mallName, String brand);
    Page<Product> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            select p
            from Product p
            where p.category is not null
              and (p.category.id = :categoryId or p.category.path like concat(:categoryPath, '>%'))
            order by p.createdAt desc
            """)
    Page<Product> findByCategoryTree(@Param("categoryId") Long categoryId, @Param("categoryPath") String categoryPath, Pageable pageable);

    @Query("""
            select new com.example.pricewatch.domain.price.batch.ProductBrandMallGroup(p.mallName, p.brand, count(p))
            from Product p
            where p.mallName is not null
              and trim(p.mallName) <> ''
              and p.brand is not null
              and trim(p.brand) <> ''
            group by p.mallName, p.brand
            order by count(p) desc
            """)
    List<ProductBrandMallGroup> findRefreshGroups(Pageable pageable);

    @Query("""
            select p
            from Product p
            where p.lastSeenAt is null or p.lastSeenAt < :threshold
            order by p.needsRematch desc, p.failCount asc, p.id asc
            """)
    List<Product> findPendingRefreshCandidates(@Param("threshold") LocalDateTime threshold, Pageable pageable);
}
