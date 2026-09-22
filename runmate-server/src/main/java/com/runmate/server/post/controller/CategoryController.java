package com.runmate.server.post.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.runmate.server.post.dto.CategoryResponse;
import com.runmate.server.post.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryMapper categoryMapper;

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryMapper.findByActiveTrueOrderByDisplayOrderAscCategoryIdAsc()
                .stream()
                .map(category -> new CategoryResponse(category.getCategoryId(), category.getName()))
                .toList();
    }
}


