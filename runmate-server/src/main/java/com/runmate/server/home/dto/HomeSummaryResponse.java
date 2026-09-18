package com.runmate.server.home.dto;

import java.util.List;

public record HomeSummaryResponse(
        long activeMemberCount,
        List<PopularCourseResponse> popularCourses,
        List<RecentPostResponse> recentPosts,
        int currentPage,
        int totalPages,
        long totalPosts
) {
    public record PopularCourseResponse(
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

    public record RecentPostResponse(
            long postId,
            String categoryName,
            String title,
            String summary,
            String authorNickname,
            String imageUrl,
            Double distanceKm,
            Integer durationSeconds,
            long likeCount,
            long commentCount
    ) {
    }
}
