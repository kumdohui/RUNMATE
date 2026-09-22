package com.runmate.server.course.dto;

public record CourseSearchRequest(
        Integer page,
        String sort,
        String keyword,
        Long categoryId,
        String region,
        String tag,
        Double minDistance,
        Double maxDistance
) {
}
