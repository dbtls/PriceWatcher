package com.example.pricewatch.domain.category.service;

import com.example.pricewatch.domain.category.entity.Category;
import com.example.pricewatch.domain.category.dto.CategoryNodeRes;
import com.example.pricewatch.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category resolveLeaf(List<String> categoryNames) {
        Category parent = null;
        Category current = null;
        int depth = 0;
        String path = "";

        for (String raw : categoryNames) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String name = raw.trim();
            depth += 1;
            path = path.isBlank() ? name : path + ">" + name;
            final Category finalParent = parent;
            final int finalDepth = depth;
            final String finalPath = path;

            current = categoryRepository.findByParentAndName(finalParent, name)
                    .orElseGet(() -> categoryRepository.save(
                            Category.builder()
                                    .name(name)
                                    .parent(finalParent)
                                    .depth(finalDepth)
                                    .path(finalPath)
                                    .build()
                    ));
            parent = current;
        }

        return current;
    }

    public List<CategoryNodeRes> getCategoryTree() {
        List<Category> categories = categoryRepository.findAllByOrderByDepthAscPathAsc();
        Map<Long, MutableCategoryNode> nodeMap = new LinkedHashMap<>();
        List<MutableCategoryNode> roots = new ArrayList<>();

        for (Category category : categories) {
            MutableCategoryNode node = new MutableCategoryNode(
                    category.getId(),
                    category.getName(),
                    category.getDepth(),
                    category.getPath()
            );
            nodeMap.put(category.getId(), node);

            if (category.getParent() == null) {
                roots.add(node);
                continue;
            }

            MutableCategoryNode parent = nodeMap.get(category.getParent().getId());
            if (parent != null) {
                parent.children.add(node);
            }
        }

        return roots.stream().map(MutableCategoryNode::toResponse).toList();
    }

    public List<CategoryNodeRes> getChildren(Long parentId) {
        return categoryRepository.findByParentIdOrderByPathAsc(parentId).stream()
                .map(category -> new CategoryNodeRes(
                        category.getId(),
                        category.getName(),
                        category.getDepth(),
                        category.getPath(),
                        List.of()
                ))
                .toList();
    }

    private static final class MutableCategoryNode {
        private final Long id;
        private final String name;
        private final int depth;
        private final String path;
        private final List<MutableCategoryNode> children = new ArrayList<>();

        private MutableCategoryNode(Long id, String name, int depth, String path) {
            this.id = id;
            this.name = name;
            this.depth = depth;
            this.path = path;
        }

        private CategoryNodeRes toResponse() {
            return new CategoryNodeRes(
                    id,
                    name,
                    depth,
                    path,
                    children.stream().map(MutableCategoryNode::toResponse).toList()
            );
        }
    }
}
