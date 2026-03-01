package com.example.pricewatch.domain.category.repository;

import com.example.pricewatch.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 카테고리 저장소.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByParentIdAndName(Long parentId, String name);
}
