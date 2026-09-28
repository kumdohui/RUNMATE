package com.runmate.server.course.dto;

import java.util.List;

public record CourseListResponse(
        List<CourseItemResponse> courses,
        int currentPage,
        int totalPages,
        long totalElements
) {
    public record CourseItemResponse(
            Long postId,
            String categoryName,
            String title,
            String summary,
            String authorNickname,
            String imageUrl,
            Double distanceKm,
            Integer durationSeconds,
            String locationName,
            String region,
            long likeCount,
            long commentCount,
            Long authorId
    ) {
    }
}
