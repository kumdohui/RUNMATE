package com.runmate.server.course.dto;

public record CourseSearchCondition(
        String keyword,
        Long categoryId,
        String region,
        String tag,
        Double minDistance,
        Double maxDistance,
        String sort,
        int limit,
        int offset
) {
}
