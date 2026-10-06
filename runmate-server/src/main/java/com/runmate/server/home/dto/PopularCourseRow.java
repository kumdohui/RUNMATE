package com.runmate.server.home.dto;

public record PopularCourseRow(
        long postId,
        String title,
        String location,
        String region,
        double distanceKm,
        int durationSeconds,
        String storageKey,
        long likeCount,
        long commentCount
) {
}
