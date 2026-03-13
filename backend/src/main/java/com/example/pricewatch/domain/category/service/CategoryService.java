package com.example.pricewatch.domain.category.service;

import com.example.pricewatch.domain.category.entity.Category;
import com.example.pricewatch.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}

