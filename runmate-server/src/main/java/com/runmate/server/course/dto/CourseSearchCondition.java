package com.runmate.server.course.dto;

import java.util.List;

public record CourseSearchCondition(
        String keyword,
        Long categoryId,
        String region,
        List<String> tags,
        Double minDistance,
        Double maxDistance,
        String sort,
        int limit,
        int offset
) {
}
