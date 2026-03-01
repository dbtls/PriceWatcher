package com.example.pricewatch.domain.category.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 카테고리 트리 엔티티.
 */
@Entity
@Table(name = "categories", uniqueConstraints = @UniqueConstraint(name = "uk_categories_parent_name", columnNames = {"parent_id", "name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(nullable = false)
    private Integer depth;

    @Column(nullable = false)
    private String path;

    /**
     * 카테고리 정적 생성 팩토리.
     */
    public static Category of(String name, Category parent, Integer depth, String path) {
        return Category.builder().name(name).parent(parent).depth(depth).path(path).build();
    }
}
