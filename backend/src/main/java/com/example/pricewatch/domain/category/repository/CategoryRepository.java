package com.example.pricewatch.domain.category.repository;

import com.example.pricewatch.domain.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByParentIdAndName(Long parentId, String name);
    Optional<Category> findByParentAndName(Category parent, String name);
    List<Category> findAllByOrderByDepthAscPathAsc();
    List<Category> findByParentIdOrderByPathAsc(Long parentId);
}
