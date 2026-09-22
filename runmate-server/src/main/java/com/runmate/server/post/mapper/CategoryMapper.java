package com.runmate.server.post.mapper;

import java.util.List;
import com.runmate.server.post.domain.Category;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper {
    Optional<Category> findById(Long categoryId);
    List<Category> findByActiveTrueOrderByDisplayOrderAscCategoryIdAsc();
}


