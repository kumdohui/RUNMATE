package com.runmate.server.home.dto;

public record PopularCourseRow(
        long postId,
        String title,
        String location,
        String region,
        double distanceKm,
        int durationSeconds,
        long likeCount,
        long commentCount
) {
}
