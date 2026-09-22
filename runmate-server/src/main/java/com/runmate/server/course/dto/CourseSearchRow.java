package com.runmate.server.course.dto;

public record CourseSearchRow(
        Long postId,
        String categoryName,
        String title,
        String summary,
        String authorNickname,
        String storageKey,
        Double distanceKm,
        Integer durationSeconds,
        String locationName,
        String region,
        long likeCount,
        long commentCount
) {
}
