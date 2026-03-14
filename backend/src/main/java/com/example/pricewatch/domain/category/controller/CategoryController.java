package com.example.pricewatch.domain.category.controller;

import com.example.pricewatch.domain.category.dto.CategoryNodeRes;
import com.example.pricewatch.domain.category.service.CategoryService;
import com.example.pricewatch.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ResponseDto<List<CategoryNodeRes>>> tree() {
        return ResponseEntity.ok(ResponseDto.success("카테고리 트리 조회 성공", categoryService.getCategoryTree()));
    }

    @GetMapping("/{parentId}/children")
    public ResponseEntity<ResponseDto<List<CategoryNodeRes>>> children(@PathVariable Long parentId) {
        return ResponseEntity.ok(ResponseDto.success("하위 카테고리 조회 성공", categoryService.getChildren(parentId)));
    }
}
